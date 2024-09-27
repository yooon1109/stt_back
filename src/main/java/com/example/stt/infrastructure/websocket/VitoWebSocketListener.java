package com.example.stt.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import reactor.core.publisher.Sinks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
@RequiredArgsConstructor
public class VitoWebSocketListener extends WebSocketListener {
    private static final Logger logger = Logger.getLogger(VitoWebSocketListener.class.getName());
    private static final int NORMAL_CLOSURE_STATUS = 1000;
    private CountDownLatch latch = null;

    private final Sinks.Many<String> sink;
    private final AtomicBoolean streaming;

    private static void log(Level level, String msg, Object... args) {
        logger.log(level, msg, args);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        log(Level.INFO, "Open " + response.message());
        latch = new CountDownLatch(1);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        if ("EOS".equals(text)) {  // "EOS" 메시지를 받으면 streaming 중지
            streaming.set(false);  // 스트리밍 중지
            sink.tryEmitComplete(); // 구독 종료
        }
        sink.tryEmitNext(text);
        log.info(text);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        log.info(bytes.hex());
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        log(Level.INFO, "Closing {0} {1}", code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        log(Level.INFO, "Closed {0} {1}", code, reason);
        latch.countDown();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        t.printStackTrace();
        latch.countDown();
    }

    public void waitClose() throws InterruptedException {
        log(Level.INFO, "Wait for finish");
        latch.await();
    }
}