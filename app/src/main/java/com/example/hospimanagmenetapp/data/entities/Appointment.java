package com.example.hospimanagmenetapp.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.sql.Time;
import java.util.Date;

@Entity(
        tableName = "appointments",
        indices = {@Index(value = {"startTime", "clinicianId"})}
)
public class Appointment {
    @PrimaryKey(autoGenerate = true) // Auto-incremented surrogate key
    public long id;                  // Local DB identifier

    public String patientNhsNumber; // link by NHS number (Lab 2 simple)
    public long startTime;           // epoch millis
    public long endTime;             // epoch millis
    public long clinicianId;         // mock doctor id
    public String clinicianName;
    public String clinic;            // location/clinic name

    public String status;            // BOOKED | CANCELLED | COMPLETED

//    @NonNull                // Must not be null; Room will enforce at runtime
//    public String ID; // Appointment ID (store digits only; format/validate in code)
//
//    public String doctor; // Doctor for the appointment; include validation for booking a doctor at the same time frame
//
//    public Date date; // Date when the appointment takes place
//    public Time startTime;
//    public Time endTime;
//
//
//    public long createdAt;         // Unix epoch millis when the record was created
//    public long updatedAt;         // Unix epoch millis when the record was last updated
}
