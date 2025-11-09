package com.example.hospimanagmenetapp.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity (
        tableName = "clinical_records"
)
public class ClinicalRecord {
    @NonNull
    public String patientNhsNumber;
    public String problems;
    public String allergies;
    public String medications;
    public long updatedAt;         // Unix epoch millis when the record was last updated
}
