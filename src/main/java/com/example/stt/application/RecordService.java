package com.example.stt.application;

import com.example.stt.domain.entity.Record;
import com.example.stt.domain.entity.RecordText;
import com.example.stt.domain.entity.RecordTextPK;
import com.example.stt.domain.repository.RecordRepository;
import com.example.stt.domain.repository.RecordTextRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class RecordService {
    private boolean stopPolling = false;
    private String transcribeId = null;
    private String accessToken = null;
    @Autowired
    private RecordRepository recordRepository;
    @Autowired
    private RecordTextRepository recordTextRepository;

    @Value("${vito.client_id}")
    String client_id;

    @Value("${vito.client_secret}")
    String client_secret;

    public String getAccessToken() throws Exception {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://openapi.vito.ai")
                .build();
        System.out.println("*****"+client_id+"******");

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

        log.info(response);
        JSONObject jsonObject = new JSONObject(response.toString());
        return jsonObject.getString("access_token");
    }

    public JSONObject transcribeFile(MultipartFile multipartFile) throws Exception {

        accessToken = getAccessToken();
        WebClient webClient = WebClient.builder()
                .baseUrl("https://openapi.vito.ai/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.MULTIPART_FORM_DATA))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                .build();

        Path currentPath = Paths.get("");
        File file = new File(currentPath.toAbsolutePath().toString() + "/"+multipartFile.getOriginalFilename());
        multipartFile.transferTo(file);

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new FileSystemResource(file));

        // 변수 설정
        boolean useDiarization = true;
        int spkCount = 2;
        boolean useDisfluencyFilter = true;
        // JSON 형식의 config 부분을 설정
        String configJson = String.format("""
            {
                "use_diarization": %b,
                "diarization": {
                  "spk_count": %d
                },
                "use_disfluency_filter": %b
            }
            """, useDiarization, spkCount, useDisfluencyFilter);

        multipartBodyBuilder.part("config", configJson)
                .contentType(MediaType.APPLICATION_JSON); // JSON 형식으로 명시적으로 설정

        MultiValueMap<String, HttpEntity<?>> parts = multipartBodyBuilder.build();
        // POST 요청 보내기
        String response = null;
        try{
            response = webClient.post()
                    .uri("/transcribe")
                    .body(BodyInserters.fromMultipartData(parts))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("post 끝");
        }catch (WebClientResponseException e){
            log.error(String.valueOf(e));
        }

        JSONObject jsonObject = new JSONObject(response.toString());

        log.info("transcribe 요청 id : " + jsonObject.getString("id"));

        stopPolling = false;
        transcribeId = jsonObject.getString("id");
        JSONObject jsonObj = startPolling();
        return jsonObj;
    }


    // 5초마다 실행 (주기는 필요에 따라 조절)
    public JSONObject startPolling() throws Exception {
        log.info("Polling 함수 첫 시작");
        String response = null;
        Thread.sleep(5000);
        JSONObject jsonObject = null;
        while (!stopPolling) {
            log.info("!!!!while polling 시작 반복중");
            response = getRecordText();

            jsonObject = new JSONObject(response.toString());
            // status 확인하여 폴링 중단 여부 결정
            if (jsonObject.getString("status").equals("completed")) {
                stopPolling = true;
            }

            try {
                Thread.sleep(5000); // 폴링 주기 (5초)를 설정
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("~~~while polling 끝 반복중");
            log.info(String.valueOf(jsonObject));


        }


        log.info("폴링함수 끝");
        return jsonObject;
    }

    public String getRecordText(){
        WebClient webClient = WebClient.builder()
                .baseUrl("https://openapi.vito.ai/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                .build();


        String uri = "/transcribe/" + transcribeId;
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public List<RecordText> saveText(JSONObject jsonObject){
        String recordId = jsonObject.getString("id");
        JSONArray results = jsonObject.getJSONObject("results").getJSONArray("utterances");
        //저장
        LocalDateTime localDateTime = LocalDateTime.now();
        // LocalDateTime을 Instant로 변환
        java.time.Instant instant = localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
        // Instant를 Date로 변환
        Date createdDate = java.util.Date.from(instant);
        Record record = new Record(recordId,createdDate,2);
        recordRepository.save(record);
        List<RecordText> recordTextList = new ArrayList<>();
        for(int i=0; i<results.length();i++){
            JSONObject jsonObj = results.getJSONObject(i);
            log.info(jsonObj.toString());
            RecordTextPK recordTextPK = new RecordTextPK(i, recordId);
            RecordText recordText = new RecordText(recordTextPK, (Integer) jsonObj.get("start_at"),jsonObj.get("spk").toString(),jsonObj.get("msg").toString());
            recordTextList.add(recordText);
//            recordTextRepository.save(recordText);
        }
        return recordTextRepository.saveAll(recordTextList);

//        // 리스트의 내용을 출력
//        for (RecordText recordText : recordTextList) {
//            System.out.println(recordText.getMsg());
//        }
    }

}
