package com.example.stt.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecordRequest {
    private String recordId;
    private String title;
    private Integer speaker;
    private MultipartFile file;
    private List<String> speakers;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<RecordText> recordTextList;
}
