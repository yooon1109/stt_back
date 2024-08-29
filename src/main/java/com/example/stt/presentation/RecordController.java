package com.example.stt.presentation;

import com.example.stt.application.RecordService;
import com.example.stt.domain.entity.Record;
import com.example.stt.domain.repository.RecordRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@RestController
public class RecordController {
    @Autowired
    RecordRepository recordRepository;
    @Autowired
    RecordService recordService;
    @GetMapping("/record")
    public ResponseEntity<List<Record>> getRecord() throws Exception {
        return ResponseEntity.status(HttpStatus.OK).body(recordRepository.findAll());
    }

    @PostMapping(value = "/save/record", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<JSONObject> saveRecord(@RequestPart MultipartFile file) throws Exception {
        return ResponseEntity.ok(recordService.transcribeFile(file));
//        return ResponseEntity.status(HttpStatus.CREATED).body(recordService.transcribeFile(file));
    }
}
