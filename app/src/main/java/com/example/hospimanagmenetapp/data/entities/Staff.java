package com.example.hospimanagmenetapp.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(tableName = "staff", indices = @Index(value = "email", unique = true))
public class Staff {

    public enum Role { ADMIN, CLINICIAN, RECEPTION }

    public enum Expertise { GP, PHYSICIAN, THERAPIST, NURSE, SURGEON }

    @PrimaryKey(autoGenerate = true) public long id;
    public String fullName; // Encrypted
    @NonNull public String email; // Encrypted
    @NonNull public Role role;
    public String adminPin; // only for ADMIN | Encrypted
    public Expertise expertise; // only for CLINICIAN
}