package com.example.stt.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecordRequest {
    private String recordId;
    private String title;
    private Integer speaker;
    private MultipartFile file;
    private List<String> speakers;
    private List<RecordText> recordTextList;
}
