package com.example.hospimanagmenetapp.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clinical_records")
public class ClinicalRecord {
    @PrimaryKey(autoGenerate = true) public long id;
    public String enPatientNhs;
    public String allergies;
    public String medications;
    public String problems;
    public long updatedAt;
}

