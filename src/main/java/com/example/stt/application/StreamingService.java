package com.example.stt.application;

import com.example.stt.infrastructure.audio.MicrophoneStreamer;
import com.example.stt.infrastructure.persistence.VitoApiService;
import com.example.stt.infrastructure.persistence.VitoWebSocketListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {
    @Autowired
    private VitoApiService vitoApiService;
    private WebSocket vitoWebSocket;
    @Autowired
    private Sinks.Many<String> sink;
    private AtomicBoolean streaming;
    private static final int SAMPLE_RATE = 44100; // 샘플링 레이트
    private static final int BITS_PER_SAMPLE = 16; // 비트 깊이
    private static final int BUFFER_SIZE = 4096;  // 버퍼 크기 설정

    private final AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE,
            BITS_PER_SAMPLE,
            1,
            1 * (BITS_PER_SAMPLE / 8),
            SAMPLE_RATE,
            false);

    @Value("${file.path}")
    private String filePath;

    public void transcribeWebSocket() throws Exception {
        streaming = new AtomicBoolean(true);  // 스트리밍을 시작할 때 true로 설정
        OkHttpClient client = new OkHttpClient();

        String token = vitoApiService.getAccessToken();

        HttpUrl.Builder httpBuilder = HttpUrl.get("https://openapi.vito.ai/v1/transcribe:streaming").newBuilder();
        httpBuilder.addQueryParameter("sample_rate", "8000");
        httpBuilder.addQueryParameter("encoding", "LINEAR16");
        httpBuilder.addQueryParameter("use_itn", "true");
        httpBuilder.addQueryParameter("use_disfluency_filter", "true");
        httpBuilder.addQueryParameter("use_profanity_filter", "false");

        String url = httpBuilder.toString().replace("https://", "wss://");

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        VitoWebSocketListener vitoWebSocketListener = new VitoWebSocketListener(sink, streaming);

        vitoWebSocket = client.newWebSocket(request, vitoWebSocketListener);

        MicrophoneStreamer microphoneStreamer = new MicrophoneStreamer();
        // ByteArrayOutputStream을 사용하여 데이터를 파일로 저장
        ByteArrayOutputStream audioOutputStream = new ByteArrayOutputStream();

        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            int readBytes;
            while (streaming.get()) {
                readBytes = microphoneStreamer.read(buffer);
                if (readBytes > 0) {
                    boolean sent = vitoWebSocket.send(ByteString.of(buffer, 0, readBytes));
                    if (!sent) {
                        System.err.println("Send buffer is full. Cannot complete request.");
                    }
                    audioOutputStream.write(buffer, 0, readBytes);
                }

            }
            microphoneStreamer.close();
            vitoWebSocket.send("EOS");
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioOutputStream.toByteArray())) {
                AudioInputStream audioStream = new AudioInputStream(byteArrayInputStream, format, audioOutputStream.size() / format.getFrameSize());
                File file = new File(filePath + File.separator + "output.wav");
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, file);
                System.out.println("Recording saved to file: " + file.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }

    // WebSocket 종료 메서드
    public void stopWebSocket() {
        if (vitoWebSocket != null) {
            vitoWebSocket.send("EOS");  // WebSocket으로 EOS 신호 전송
            streaming.set(false);   // 스트리밍 종료
        }
    }


}
