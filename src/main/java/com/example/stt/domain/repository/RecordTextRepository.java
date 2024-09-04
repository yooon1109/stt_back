package com.example.stt.domain.repository;

import com.example.stt.domain.entity.RecordText;
import com.example.stt.domain.entity.RecordTextPK;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordTextRepository extends JpaRepository<RecordText, RecordTextPK> {

    List<RecordText> findByRecordTextPK_RecordId(String recordId);
}
