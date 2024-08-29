package com.example.stt.domain.repository;

import com.example.stt.domain.entity.RecordText;
import com.example.stt.domain.entity.RecordTextPK;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordTextRepository extends JpaRepository<RecordText, RecordTextPK> {

}
