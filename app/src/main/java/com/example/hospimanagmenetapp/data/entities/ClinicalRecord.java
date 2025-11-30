package com.example.hospimanagmenetapp.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "clinical_records",
        // Relationship: One-to-One with Patient
        // Each clinical record belongs to exactly one patient.
        foreignKeys = @ForeignKey(entity = Patient.class,
                parentColumns = "enPatientNhsNumber",
                childColumns = "enPatientNhs",
                onDelete = ForeignKey.CASCADE), // If the patient is deleted, their record is also deleted.
        indices = {@Index(value = "enPatientNhs", unique = true)} // A patient can only have one record.
)
public class ClinicalRecord {
    @PrimaryKey(autoGenerate = true) public long id;
    public String enPatientNhs; // Encrypted - Foreign Key to Patient
    public String allergies;
    public String medications;
    public String problems;
    public long updatedAt;
}

