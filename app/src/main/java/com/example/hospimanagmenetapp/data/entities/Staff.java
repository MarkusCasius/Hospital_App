package com.example.hospimanagmenetapp.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Represents a single staff member.
// Primary Entity referenced by Appointment Table
@Entity(tableName = "staff", indices = @Index(value = "email", unique = true))
public class Staff {

    public enum Role { ADMIN, CLINICIAN, RECEPTION }

    public enum Expertise { GP, PHYSICIAN, THERAPIST, NURSE, SURGEON }

    @PrimaryKey(autoGenerate = true)
    public long id; // Used as foreign key by Appointment
    public String fullName; // Encrypted
    @NonNull public String email; // Encrypted
    @NonNull public Role role; // Determines permissions of staff member
    public String adminPin; // only for ADMIN | Encrypted
    public Expertise expertise; // only for CLINICIAN
}