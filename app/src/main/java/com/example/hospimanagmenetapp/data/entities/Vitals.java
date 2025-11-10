package com.example.hospimanagmenetapp.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vitals")
public class Vitals {
    @PrimaryKey(autoGenerate = true) public long id;
    public String patientNhs;
    public float temperature;
    public int heartRate;
    public int systolic;
    public int diastolic;
    public long timestamp;
    public boolean synced; // false = pending upload
}

