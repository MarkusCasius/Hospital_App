package com.example.hospimanagmenetapp.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "vitals",
        // Relationship: Many-to-One with Patient
        // Many vitals records can belong to one patient.
        foreignKeys = @ForeignKey(entity = Patient.class,
                parentColumns = "enPatientNhsNumber",
                childColumns = "enPatientNhsNumber",
                onDelete = ForeignKey.CASCADE), // If the patient is deleted, all their vitals records are deleted.
        indices = {@Index("enPatientNhsNumber")}
)
public class Vitals {
    @PrimaryKey(autoGenerate = true) public long id;
    public String enPatientNhsNumber; // Foreign Key to Patient - Encrypted
    public float temperature;
    public int heartRate;
    public int systolic;
    public int diastolic;
    public long timestamp;
    public boolean synced; // false = pending upload
}

