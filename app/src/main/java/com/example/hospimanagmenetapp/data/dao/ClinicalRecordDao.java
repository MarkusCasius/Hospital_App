package com.example.hospimanagmenetapp.data.dao;

import androidx.room.*;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import java.util.List;

@Dao
public interface ClinicalRecordDao {
    @Query("SELECT * FROM clinical_records WHERE patientNhsnumber=:nhs LIMIT 1")
    ClinicalRecord findByPatient(String nhs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ClinicalRecord record);
}
