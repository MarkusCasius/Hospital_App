package com.example.hospimanagmenetapp.data.entities; // Entity lives in the data.entities package

import androidx.annotation.NonNull;  // Annotation to mark fields that must not be null
import androidx.room.Entity;         // Marks this class as a Room table
import androidx.room.Index;          // Allows creating DB indices for faster lookups/uniqueness
import androidx.room.PrimaryKey;     // Identifies the primary key column

@Entity(
        tableName = "patients",                                  // Actual SQLite table name
        indices = {@Index(value = {"nhsNumber"}, unique = true)} // Unique index so each NHS number appears only once
)
public class Patient {
    @PrimaryKey(autoGenerate = true) // Auto-incremented surrogate key
    public long id;                  // Local DB identifier

    @NonNull                // Must not be null; Room will enforce at runtime
    public String nhsNumber; // NHS number (store digits only; format/validate in code)

    public String fullName;        // Patient’s full name (consider @NonNull if mandatory)
    public String dateOfBirth;     // ISO yyyy-MM-dd for simplicity; a TypeConverter to Date is cleaner
    public String phone;           // Contact number (normalised/validated in code)
    public String email;           // Contact email (basic pattern check in code/UI)

    public long createdAt;         // Unix epoch millis when the record was created
    public long updatedAt;         // Unix epoch millis when the record was last updated
}