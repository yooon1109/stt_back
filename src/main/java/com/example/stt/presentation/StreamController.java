package com.example.stt.presentation;

import com.example.stt.application.StreamingService;
import com.example.stt.infrastructure.persistence.VitoApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class StreamController {

    @Autowired
    private Sinks.Many<String> sink;

    @Autowired
    private StreamingService streamingService;
    @Autowired
    private VitoApiService vitoApiService;


    @GetMapping(value = "/stream")
    public Flux<String> streamEvents() throws Exception{
        streamingService.transcribeWebSocket();
        return sink.asFlux()
                .takeWhile(data -> !data.equals("EOS"));
    }


    @GetMapping(value = "/token")
    public ResponseEntity<String> getToken() throws Exception{

        return ResponseEntity.status(HttpStatus.OK).body(vitoApiService.getAccessToken());
    }

    @PostMapping("/stop")
    public ResponseEntity<Void> stopStreaming() {
        streamingService.stopWebSocket();  // WebSocket 종료
        sink.tryEmitComplete();  // 구독 종료
        return ResponseEntity.ok().build();
    }
}
