package com.example.stt.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.springframework.context.annotation.Bean;

public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {

        Info info = new Info()
                .version("v1.0.0")
                .title("API TEST")
                .description("연습용");

        // 파일 업로드를 위한 Content 객체 생성
        Content content = new Content()
                .addMediaType("multipart/form-data", new MediaType());

        // 파일 업로드를 지원하는 오퍼레이션(Operation) 설정
        Operation uploadOperation = new Operation()
                .requestBody(new RequestBody().content(content));


        return new OpenAPI()
                .info(info)
                .path("/api/upload", new io.swagger.v3.oas.models.PathItem().post(uploadOperation));

    }
}
