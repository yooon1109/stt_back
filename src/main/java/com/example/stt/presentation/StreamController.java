package com.example.stt.presentation;

import com.example.stt.application.StreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@RequiredArgsConstructor
public class StreamController {

    @Autowired
    private final Sinks.Many<String> sink;

    @Autowired
    private StreamingService streamingService;

    @GetMapping(value = "/stream")
    public Flux<String> streamEvents() throws Exception{
        streamingService.transcribeWebSocket();
        return sink.asFlux();
    }
}
