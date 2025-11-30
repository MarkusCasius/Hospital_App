package com.example.hospimanagmenetapp.data; // Package for data-layer classes

import androidx.room.Database;          // Room annotation to define the DB schema
import androidx.room.Room;              // Factory for creating Room databases
import androidx.room.RoomDatabase;      // Base class for Room databases
import android.content.Context;         // Needed to build the DB with an app Context

import com.example.hospimanagmenetapp.data.dao.AppointmentDao;
import com.example.hospimanagmenetapp.data.dao.ClinicDao;
import com.example.hospimanagmenetapp.data.dao.ClinicalRecordDao;
import com.example.hospimanagmenetapp.data.dao.PatientDao; // DAO for Patient operations
import com.example.hospimanagmenetapp.data.dao.StaffDao;   // DAO for Staff operations
import com.example.hospimanagmenetapp.data.dao.VitalsDao;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.entities.Clinic;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import com.example.hospimanagmenetapp.data.entities.Patient; // Entity mapped to a table
import com.example.hospimanagmenetapp.data.entities.Staff;   // Entity mapped to a table
import com.example.hospimanagmenetapp.data.entities.Vitals;

/**
 * Defines the application's database schema and serves as the main access point to the persisted data.
 *
 * This database manages the core entities of the hospital management system and their relationships:
 *
 * ### Primary Entities:
 * - {@link Patient}: The central entity representing a patient. Identified by a unique NHS number.
 * - {@link Staff}: Represents employees (Admins, Clinicians, etc.). Identified by a unique auto-generated ID.
 * - {@link Clinic}: Represents a physical clinic location. Identified by a unique name.
 *
 * ### Relationships:
 *
 * 1.  **Appointment Relationships (Many-to-One):**
 *     - An {@link Appointment} has a **Many-to-One** relationship with {@link Patient}. (Many appointments can belong to one patient).
 *     - An {@link Appointment} has a **Many-to-One** relationship with {@link Staff}. (Many appointments can be assigned to one clinician).
 *     - An {@link Appointment} has a **Many-to-One** relationship with {@link Clinic}. (Many appointments can occur at one clinic).
 *
 * 2.  **Clinical Record Relationship (One-to-One):**
 *     - A {@link ClinicalRecord} has a **One-to-One** relationship with {@link Patient}. (Each patient has exactly one clinical record).
 *
 * 3.  **Vitals Relationship (One-to-Many):**
 *     - A {@link Vitals} record has a **Many-to-One** relationship with {@link Patient}. (This is the inverse of One-to-Many: A patient can have many vitals records).
 *
 * ### Integrity Rules (defined by Foreign Keys):
 * - If a `Patient` is deleted, all of their associated `Appointments`, `ClinicalRecords`, and `Vitals` are also deleted (`onDelete = CASCADE`).
 * - If a `Staff` member (clinician) is deleted, the `clinicianId` in their associated `Appointments` is set to `NULL` (`onDelete = SET_NULL`).
 * - A `Clinic` cannot be deleted if it still has associated `Appointments` (`onDelete = RESTRICT`).
 */

@Database(entities = {Patient.class, Staff.class, Appointment.class, ClinicalRecord.class, Vitals.class, Clinic.class}, version = 3, exportSchema = false)
//  Declares the Room database: which entities it manages, the schema version,
//   and whether to export the schema as JSON for tooling (false = do not export).
public abstract class AppDatabase extends RoomDatabase { // Concrete DB extends RoomDatabase

    // Singleton instance (volatile ensures visibility across threads)
    private static volatile AppDatabase INSTANCE;

    // Room generates the implementation; these expose your DAOs to callers
    public abstract PatientDao patientDao();
    public abstract StaffDao staffDao();
    public abstract AppointmentDao appointmentDao();
    public abstract ClinicalRecordDao clinicalRecordDao();
    public abstract VitalsDao vitalsDao();
    public abstract ClinicDao clinicDao();


    // Thread-safe double-checked locking to get/create the singleton DB
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) { // Fast path: already created?
            synchronized (AppDatabase.class) { // Serialise creation across threads
                if (INSTANCE == null) { // Second check inside the lock
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(), // Use app Context to avoid Activity leaks
                                    AppDatabase.class,               // The RoomDatabase subclass to create
                                    "hms_db"                         // On-device filename for the DB
                            )
                            .fallbackToDestructiveMigration()       // Wipes & rebuilds on version change if no migration (dev-friendly, data-loss risk)
                            .build();                               // Build the database instance
                }
            }
        }
        return INSTANCE; // Return the shared database
    }
}