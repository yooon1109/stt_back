package com.example.stt.domain.repository;

import com.example.stt.domain.entity.Record;
import com.example.stt.domain.entity.RecordTextPK;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<Record, String> {
}
