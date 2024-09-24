package com.example.stt.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Record {
    @Id
    private String id;
    private Date recordDate;
    private Integer speaker;
    private String title;
    private String speakers;
    private String recordFilename;
    private String recordType;
    private Double duration;
}

