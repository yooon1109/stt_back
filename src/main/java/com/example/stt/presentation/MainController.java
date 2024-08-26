package com.example.stt.presentation;

import com.example.stt.domain.entity.Record;
import com.example.stt.domain.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@RestController
public class MainController {
    @Autowired
    RecordRepository recordRepository;

    @PostMapping("/test/write")
    public ResponseEntity<Record> saveRecord() throws Exception {
        Record record = new Record();
        LocalDateTime localDateTime = LocalDateTime.now();
        // LocalDateTime을 Instant로 변환
        java.time.Instant instant = localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
        // Instant를 Date로 변환
        Date createdDate = java.util.Date.from(instant);
        record.setRecordDate(createdDate);

        Record savedrecord = recordRepository.save(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedrecord);
    }

    @GetMapping("/test/get")
    public ResponseEntity<List<Record>> getRecords(){
        List<Record> recordList = recordRepository.findAll();
        return ResponseEntity.ok(recordList);
    }


}
