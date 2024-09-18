package com.example.stt.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Sinks;

import org.springframework.http.HttpHeaders;

import javax.sound.sampled.*;
import java.io.*;

import java.util.concurrent.atomic.AtomicBoolean;
@Component
@RequiredArgsConstructor
public class AudioWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private VitoApiService vitoApiService;
    private WebSocket vitoWebSocket;
    @Autowired
    private Sinks.Many<String> sink;
    private AtomicBoolean streaming;
    private File audioFile;
    private AudioFormat format;
    private TargetDataLine microphone;
    private static final int BUFFER_SIZE = 4096;  // 버퍼 크기 설정
    // Buffer to store incoming audio data
    private ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

    public void MicrophoneStreamer(WebSocket vitoWebSocket) throws LineUnavailableException {
        this.vitoWebSocket = vitoWebSocket;

        // 오디오 포맷 설정
        format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100,  // SAMPLE_RATE
                16,     // BITS_PER_SAMPLE
                1,      // 모노
                2,      // 2바이트 (16비트)
                44100,  // SAMPLE_RATE
                false   // Big-endian
        );

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone not supported");
        }

        // 마이크 준비 및 시작
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] payload = message.getPayload().array();

        // 데이터를 처리하는 로직
        try {
            processAudioData(payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processAudioData(byte[] data) throws IOException {
        audioBuffer.write(data);

        int offset = 0;
        int length = data.length;

        // 데이터가 BUFFER_SIZE보다 클 경우 여러 번 나누어서 전송
        while (length > 0) {
            int chunkSize = Math.min(length, BUFFER_SIZE);
            ByteString byteString = ByteString.of(data, offset, chunkSize);

            boolean sent = vitoWebSocket.send(byteString);
            if (!sent) {
                System.err.println("Send buffer is full. Cannot complete request.");
                break; // 버퍼가 가득 찼을 때 더 이상 데이터를 보내지 않음
            }

            // 다음 청크를 위해 오프셋과 길이 업데이트
            offset += chunkSize;
            length -= chunkSize;
        }


    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        System.out.println("WebSocket connection established");

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

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket connection closed");
        handleEndOfStream();
    }

    private void handleEndOfStream() {
        try {
            byte[] audioData = audioBuffer.toByteArray();
            audioBuffer.reset(); // Clear buffer

            // Save audio data to a file
            audioFile = new File("recorded_audio.wav");
            try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                fos.write(audioData);
            }

            System.out.println("Final audio data saved to: " + audioFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error handling end of stream: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error: " + exception.getMessage());
    }

    public ResponseEntity<InputStreamResource> getAudioFile() throws IOException {
        if (audioFile != null && audioFile.exists()) {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(audioFile));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + audioFile.getName());

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
