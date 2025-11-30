package com.example.hospimanagmenetapp.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Represents a single appointment record in database
// Entity links Patient, Staff (Clinicians) and Clinics
@Entity(tableName = "appointments",
        foreignKeys = {
                // Relationship: Many-to-One with Patient
                // Each appointment belongs to one patient. A patient can have many appointments.
                @ForeignKey(entity = Patient.class,
                        parentColumns = "enPatientNhsNumber", // Column in the 'patients' table
                        childColumns = "enPatientNhsNumber",  // Column in this 'appointments' table
                        onDelete = ForeignKey.CASCADE),       // If a Patient is deleted, delete their appointments.

                // Relationship: Many-to-One with Staff
                // Each appointment is assigned to one clinician. A clinician can have many appointments.
                @ForeignKey(entity = Staff.class,
                        parentColumns = "id",           // Column in the 'staff' table
                        childColumns = "clinicianId",   // Column in this 'appointments' table
                        onDelete = ForeignKey.SET_NULL),// If a Staff member is deleted, set clinicianId to null.

                // Relationship: Many-to-One with Clinic
                // Each appointment belongs to one clinic. A clinic can have many appointments.
                @ForeignKey(entity = Clinic.class,
                        parentColumns = "name",         // Column in the 'clinics' table
                        childColumns = "clinic",        // Column in this 'appointments' table
                        onDelete = ForeignKey.RESTRICT) // Prevent deleting a Clinic if it still has appointments.
        },
        indices = {
                @Index("enPatientNhsNumber"),
                @Index("clinicianId"),
                @Index("clinic")
        }
)
public class Appointment {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String enPatientNhsNumber; // link by NHS number (Lab 2 simple) - Foreign Key to Patient - Encrypted
    public long startTime;           // epoch millis
    public long endTime;             // epoch millis
    public long clinicianId;         // mock doctor id - Foreign Key to Staff
    public String enClinicianName;   // Encrypted
    public String clinic;            // location/clinic name - Foreign Key to Clinic

    public String status;            // BOOKED | CANCELLED | COMPLETED
}
