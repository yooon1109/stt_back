package com.example.stt.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

@Configuration
public class SinkConfig {
    @Bean
    public Sinks.Many<String> stringSink() {
        return Sinks.many().unicast().onBackpressureBuffer();
    }
}
