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
    private List<RecordText> recordTextList;
}
