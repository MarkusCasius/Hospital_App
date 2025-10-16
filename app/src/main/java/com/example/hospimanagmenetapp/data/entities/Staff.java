package com.example.hospimanagmenetapp.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;

@Entity(tableName = "staff", indices = @Index(value = "email", unique = true))
public class Staff {

    public enum Role { ADMIN, CLINICIAN, RECEPTION }

    @PrimaryKey(autoGenerate = true) public long id;
    public String fullName;
    @NonNull public String email;
    @NonNull public Role role;
    public String adminPin; // only for ADMIN

    // ----- Inline converters (no new class) -----
    @TypeConverter
    public static String fromRole(Role role) {
        return role == null ? null : role.name();
    }

    @TypeConverter
    public static Role toRole(String value) {
        return value == null ? null : Role.valueOf(value);
    }
}