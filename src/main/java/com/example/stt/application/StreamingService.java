package com.example.stt.application;

import com.example.stt.infrastructure.audio.MicrophoneStreamer;
import com.example.stt.infrastructure.persistence.VitoWebSocketListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingService {
    @Autowired
    private VitoApiService vitoApiService;
    private WebSocket vitoWebSocket;

    private static final int BUFFER_SIZE = 1024;

    public void transcribeWebSocket() throws Exception {
        final AtomicBoolean streaming = new AtomicBoolean(true);  // 스트리밍을 시작할 때 true로 설정
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

        VitoWebSocketListener webSocketListener = new VitoWebSocketListener();
        WebSocket webSocket = client.newWebSocket(request, webSocketListener);

        MicrophoneStreamer microphoneStreamer = new MicrophoneStreamer();

        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            int readBytes;
            while (streaming.get()) {
                readBytes = microphoneStreamer.read(buffer);
                if (readBytes > 0) {
                    boolean sent = webSocket.send(ByteString.of(buffer, 0, readBytes));
                    if (!sent) {
                        System.err.println("Send buffer is full. Cannot complete request.");
                    }
                }
            }
            microphoneStreamer.close();
            webSocket.send("EOS");
        }).start();

    }
}
