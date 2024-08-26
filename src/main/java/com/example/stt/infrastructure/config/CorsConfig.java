package com.example.stt.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS를 활성화
                .allowedOrigins("*") // 허용할 Origin(도메인)을 지정
                .allowedMethods("GET", "POST", "PUT", "DELETE"); // 허용할 HTTP 메서드를 지정
    }
}
