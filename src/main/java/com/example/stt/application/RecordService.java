package com.example.stt.application;

import com.example.stt.domain.entity.Record;
import com.example.stt.domain.entity.RecordResponse;
import com.example.stt.domain.entity.RecordText;
import com.example.stt.domain.entity.RecordTextPK;
import com.example.stt.domain.repository.RecordRepository;
import com.example.stt.domain.repository.RecordTextRepository;
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
import java.util.*;

@Service
@Slf4j
public class RecordService {
    @Autowired
    private RecordRepository recordRepository;
    @Autowired
    private RecordTextRepository recordTextRepository;
    @Autowired
    private VitoApiService vitoApiService;

    public RecordResponse transcribeFile(MultipartFile multipartFile, int spk) throws Exception {

        // Vito API 호출을 분리하여 처리
        JSONObject jsonObj = vitoApiService.transcribeFile(multipartFile, spk);

        String id = jsonObj.getString("id");
        String status = jsonObj.getString("status");

        JSONArray utterances = jsonObj.getJSONObject("results").getJSONArray("utterances");
        List<RecordText> recordTextList = arrayToList(utterances, id);

//        //저장
//        LocalDateTime localDateTime = LocalDateTime.now();
//        // LocalDateTime을 Instant로 변환
//        java.time.Instant instant = localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
//        // Instant를 Date로 변환
//        Date createdDate = java.util.Date.from(instant);
//        Record record = new Record(id,createdDate,spk);
//        recordRepository.save(record);

        return new RecordResponse(id, status, recordTextList);
    }

    public List<RecordText> saveText(Map<String, Object> jsonObject, String title){
        Object recordId = jsonObject.get("id");
        Map<String, Object> results = (Map<String, Object>) jsonObject.get("results");
        //저장
        LocalDateTime localDateTime = LocalDateTime.now();
        // LocalDateTime을 Instant로 변환
        java.time.Instant instant = localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
        // Instant를 Date로 변환
        Date createdDate = java.util.Date.from(instant);
        Record record = new Record((String) recordId,createdDate,2,title);
        recordRepository.save(record);
        List<RecordText> recordTextList = new ArrayList<>();
        for(int i=0; i<results.size();i++){
            Map jsonObj = (Map) results.get(i);
            log.info(jsonObj.toString());
            RecordTextPK recordTextPK = new RecordTextPK(i, (String) recordId);
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

    private List<RecordText> arrayToList(JSONArray utterances, String recordId) {
        List<RecordText> recordTextList = new ArrayList<>();
        for (int i = 0; i < utterances.length(); i++) {
            JSONObject utteranceObj = utterances.getJSONObject(i);
            RecordTextPK recordTextPK = new RecordTextPK(i, recordId);
            RecordText recordText = new RecordText(recordTextPK,
                    utteranceObj.getInt("start_at"),
                    utteranceObj.get("spk").toString(),
                    utteranceObj.getString("msg"));
            recordTextList.add(recordText);
        }
        return recordTextList;
    }
}
