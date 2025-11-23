package com.example.hospimanagmenetapp.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;

import org.checkerframework.common.aliasing.qual.Unique;

@Entity(tableName = "staff", indices = @Index(value = "email", unique = true))
public class Staff {

    public enum Role { ADMIN, CLINICIAN, RECEPTION }

    @PrimaryKey(autoGenerate = true) public long id;
    public String fullName;
    @NonNull @Unique public String email;
    @NonNull public Role role;
    public String adminPin; // only for ADMIN
}