package com.example.hospimanagmenetapp.data.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "appointments",
        indices = {@Index(value = {"startTime","clinicianId"})})
public class Appointment {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String patientNhsNumber; // link by NHS number (Lab 2 simple)
    public long startTime;           // epoch millis
    public long endTime;             // epoch millis
    public long clinicianId;         // mock doctor id
    public String clinicianName;
    public String clinic;            // location/clinic name

    public String status;            // BOOKED | CANCELLED | COMPLETED
}
