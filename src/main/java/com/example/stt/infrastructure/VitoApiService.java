package com.example.stt.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;

@Service
@Slf4j
public class VitoApiService {
    @Value("${vito.client_id}")
    private String client_id;

    @Value("${vito.client_secret}")
    private String client_secret;

    private String accessToken;

    // 액세스 토큰 가져오기
    public String getAccessToken() throws Exception {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://openapi.vito.ai")
                .build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", client_id);
        formData.add("client_secret", client_secret);

        String response = webClient
                .post()
                .uri("/v1/authenticate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject jsonObject = new JSONObject(response);
        return jsonObject.getString("access_token");
    }

    // Transcribe 파일 처리
    public JSONObject transcribeFile(MultipartFile multipartFile, Integer spk) throws Exception {
        accessToken = getAccessToken();
        WebClient webClient = WebClient.builder()
                .baseUrl("https://openapi.vito.ai/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.MULTIPART_FORM_DATA))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                .build();

        File file = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());
        multipartFile.transferTo(file);

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new FileSystemResource(file));

        String configJson;
        if(spk!=null){
            configJson = String.format("""
            {
                "use_diarization": %b,
                "diarization": {
                  "spk_count": %d
                },
                "use_disfluency_filter": %b
            }
            """, true, spk, true);
        }else {
            configJson = String.format("""
            {
                "use_disfluency_filter": %b
            }
            """, true);
        }


        multipartBodyBuilder.part("config", configJson)
                .contentType(MediaType.APPLICATION_JSON);

        MultiValueMap<String, HttpEntity<?>> parts = multipartBodyBuilder.build();
        String response = webClient.post()
                .uri("/transcribe")
                .body(BodyInserters.fromMultipartData(parts))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject jsonObj = startPolling(new JSONObject(response.toString()).getString("id"));
        return jsonObj;
    }

    // 5초마다 폴링하여 상태 확인
    public JSONObject startPolling(String transcribeId) throws Exception {
        log.info("Polling 시작");
        WebClient webClient = WebClient.builder()
                .baseUrl("https://openapi.vito.ai/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                .build();

        String response = null;
        JSONObject jsonObject = null;
        boolean stopPolling = false;
        while (!stopPolling) {
            log.info("폴링 중...");
            response = webClient.get()
                    .uri("/transcribe/" + transcribeId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            jsonObject = new JSONObject(response);

            if (jsonObject.getString("status").equals("completed")) {
                stopPolling = true;
            } else {
                Thread.sleep(5000); // 5초 대기
            }
        }
        log.info("폴링 종료");
        return jsonObject;
    }
}

