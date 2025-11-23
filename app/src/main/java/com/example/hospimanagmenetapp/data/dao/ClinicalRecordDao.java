package com.example.hospimanagmenetapp.data.dao;

import androidx.room.*;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import java.util.List;

@Dao
public interface ClinicalRecordDao {
    @Query("SELECT * FROM clinical_records WHERE enPatientNhs=:nhs LIMIT 1")
    ClinicalRecord findByPatient(String nhs);

    @Query("SELECT * FROM clinical_records")
    List<ClinicalRecord> getAllRecords();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ClinicalRecord record);
}

