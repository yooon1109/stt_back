package com.example.stt.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecordResponse{
    private String id;
    private String status;
    private String title;
    private List<RecordText> recordTextList;
    private Integer speaker;
    private List<String> speakers;
    private byte[] fileData;
    private String recordName;
    private String recordType;
    private Double duration;
}
