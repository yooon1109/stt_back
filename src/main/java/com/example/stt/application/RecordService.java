package com.example.stt.application;

import com.example.stt.domain.entity.*;
import com.example.stt.domain.entity.Record;
import com.example.stt.domain.repository.RecordRepository;
import com.example.stt.domain.repository.RecordTextRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public RecordResponse transcribeFile(RecordRequest recordRequest) throws Exception {

        // Vito API 호출을 분리하여 처리
        JSONObject jsonObj = vitoApiService.transcribeFile(recordRequest.getFile(), recordRequest.getSpeaker());

        String id = jsonObj.getString("id");
        String status = jsonObj.getString("status");

        JSONArray utterances = jsonObj.getJSONObject("results").getJSONArray("utterances");
        RecordResponse recordTextList = saveText((Map<String, Object>) utterances, recordRequest);

        return recordTextList;
    }

    public RecordResponse saveText(Map<String, Object> jsonObject, RecordRequest recordRequest){
        Object recordId = jsonObject.get("id");
        Map<String, Object> results = (Map<String, Object>) jsonObject.get("results");
        //저장
        LocalDateTime localDateTime = LocalDateTime.now();
        // LocalDateTime을 Instant로 변환
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        // Instant를 Date로 변환
        Date createdDate = Date.from(instant);

        String speakersString = null;
        if(!recordRequest.getRecordId().isEmpty()){
            speakersString = String.join(",", recordRequest.getSpeakers());
        }

        Record record = new Record((String) recordId, createdDate, recordRequest.getSpeaker(), recordRequest.getTitle(), speakersString);
        Record responseRecord = recordRepository.save(record);

        List<RecordText> recordTextList = new ArrayList<>();
        for(int i=0; i<results.size();i++){
            Map jsonObj = (Map) results.get(i);
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
