package com.example.stt.application;

import com.example.stt.domain.entity.*;
import com.example.stt.domain.entity.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.example.stt.domain.repository.RecordRepository;
import com.example.stt.domain.repository.RecordTextRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Value("${file.path}")
    private String filePath;

    public RecordResponse transcribeFile(RecordRequest recordRequest) throws Exception {
        // 파일을 특정 경로에 저장
        saveFile(recordRequest.getFile(), filePath);
        // Vito API 호출을 분리하여 처리
        JSONObject jsonObj = vitoApiService.transcribeFile(recordRequest.getFile(), recordRequest.getSpeaker());

        String id = jsonObj.getString("id");
        String status = jsonObj.getString("status");

        Map<String, Object> map = jsonObj.toMap();
        RecordResponse recordTextList = saveText(jsonObj, recordRequest);

        return recordTextList;
    }

    private void saveFile(MultipartFile file, String path) throws IOException {
        // 파일이 비어있지 않은 경우에만 저장
        if (!file.isEmpty()) {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            // 파일 경로 설정
            String filePath = path + File.separator + fileName;
//            File directory = new File(path);

            Path destinationPath = Paths.get(filePath);

            // 파일을 경로에 저장
            Files.write(destinationPath, file.getBytes()); // 파일 저장
        } else {
            throw new IOException("File is empty!");
        }
    }

    public RecordResponse saveText(JSONObject jsonObject, RecordRequest recordRequest){
        Object recordId = jsonObject.get("id");
        JSONArray results = jsonObject.getJSONObject("results").getJSONArray("utterances");
        //저장
        LocalDateTime localDateTime = LocalDateTime.now();
        // LocalDateTime을 Instant로 변환
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        // Instant를 Date로 변환
        Date createdDate = Date.from(instant);

        String speakersString = null;
        if(!recordRequest.getSpeakers().isEmpty()){
            speakersString = String.join(",", recordRequest.getSpeakers());
        }

        Record record = new Record((String) recordId, createdDate, recordRequest.getSpeaker(), recordRequest.getTitle(), speakersString);
        Record responseRecord = recordRepository.save(record);

        List<RecordText> recordTextList = new ArrayList<>();
        for(int i=0; i<results.length();i++){
            JSONObject jsonObj = results.getJSONObject(i);

            log.info(jsonObj.toString());
            RecordTextPK recordTextPK = new RecordTextPK(i, (String) recordId);
            RecordText recordText = new RecordText(recordTextPK, (Integer) jsonObj.get("start_at"),jsonObj.get("spk").toString(),jsonObj.get("msg").toString());
            recordTextList.add(recordText);
        }
        List<RecordText> responseRecords = recordTextRepository.saveAll(recordTextList);

        return new RecordResponse(responseRecord.getId(), "success", responseRecord.getTitle(), responseRecords, responseRecord.getSpeaker(), recordRequest.getSpeakers());

    }

    public RecordResponse editText(RecordRequest recordRequest){
        Record record = recordRepository.findById(recordRequest.getRecordId()).get();
        if(recordRequest.getSpeaker()!=null){
            record.setSpeaker(recordRequest.getSpeaker());
        }
        if(!recordRequest.getTitle().isEmpty()){
            record.setTitle(recordRequest.getTitle());
        }

        if(!recordRequest.getSpeakers().isEmpty()){
            String speakersString = String.join(",", recordRequest.getSpeakers());
            record.setSpeakers(speakersString);
        }
        recordRepository.save(record);

        List<RecordText> requestList = recordRequest.getRecordTextList();
        for(int i=0; i<requestList.size(); i++){
            RecordTextPK pk = new RecordTextPK(i, recordRequest.getRecordId());
            requestList.get(i).setRecordTextPK(pk);
        }


        List<RecordText> recordTexts = recordTextRepository.findByRecordTextPK_RecordId(  recordRequest.getRecordId());
        recordTextRepository.deleteAll(recordTexts);
        List<RecordText> response = recordTextRepository.saveAll(requestList);
        return new RecordResponse(recordRequest.getRecordId(), "success", recordRequest.getTitle(), response,null,null);
    }

    public RecordResponse recordDetail(String recordId){
        Record record = recordRepository.findById(recordId).get();
        List<RecordText> recordTexts = recordTextRepository.findByRecordTextPK_RecordId(recordId);

        List<String> speakers = null;
        if(!record.getSpeakers().isEmpty()){
            speakers = Arrays.stream(record.getSpeakers().split(",")).toList();
        }


        return new RecordResponse(recordId,"success",record.getTitle(), recordTexts,record.getSpeaker(),speakers);
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
