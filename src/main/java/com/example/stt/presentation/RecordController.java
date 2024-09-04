package com.example.stt.presentation;

import com.example.stt.application.RecordService;
import com.example.stt.domain.entity.Record;
import com.example.stt.domain.entity.RecordRequest;
import com.example.stt.domain.entity.RecordResponse;
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
import java.util.Map;
import java.util.Objects;

@RestController
public class RecordController {
    @Autowired
    RecordRepository recordRepository;
    @Autowired
    RecordService recordService;

    @GetMapping("/records")
    public ResponseEntity<List<Record>> getRecord() throws Exception {
        return ResponseEntity.status(HttpStatus.OK).body(recordRepository.findAll());
    }

    @GetMapping("/record/detail")
    public ResponseEntity<RecordResponse> getRecordDetail(@RequestParam String recordId) throws Exception {
        return ResponseEntity.status(HttpStatus.OK).body(recordService.recordDetail(recordId));
    }

    @PostMapping(value = "/save/record", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RecordResponse> saveRecord(@ModelAttribute RecordRequest recordRequest) throws Exception {
        return ResponseEntity.status(HttpStatus.OK).body(recordService.transcribeFile(recordRequest));
    }

    @PostMapping(value = "/edit/record")
    public ResponseEntity<RecordResponse>  editRecord(@RequestBody RecordRequest recordRequest) throws Exception {
        return ResponseEntity.status(HttpStatus.OK).body(recordService.editText(recordRequest));
    }
}
