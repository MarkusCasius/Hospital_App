package com.example.hospimanagmenetapp.data.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Represents physical clinic location
// Primary entity referenced by Appointment table
@Entity(tableName = "clinics", indices = {@Index(value = "name", unique = true)})
public class Clinic {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String location;
}