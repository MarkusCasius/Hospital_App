package com.example.hospimanagmenetapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.data.entities.Staff;

import java.util.concurrent.Executors;


public class DatabaseSeeder {

    private static final String PREFS_NAME = "SeederPrefs";
    private static final String KEY_SEEDED = "databaseSeeded";
    private static final String TAG = "DatabaseSeeder";

    public static void seed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean alreadySeeded = prefs.getBoolean(KEY_SEEDED, false);

        if (alreadySeeded) {
            Log.d(TAG, "Database has already been seeded. Skipping.");
            return;
        }

        Log.d(TAG, "Database not seeded. Starting seed process.");
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            try {
                seedStaff(context, db);

                seedPatients(context, db);

                seedAppointments(db);

                prefs.edit().putBoolean(KEY_SEEDED, true).apply();
                Log.i(TAG, "Database successfully seeded.");

            } catch (Exception e) {
                Log.e(TAG, "Failed to seed database", e);
            }
        });
    }

    private static void seedStaff(Context context, AppDatabase db) throws Exception {
        Log.d(TAG, "Seeding staff...");
        EncryptionManager encryptionManager = new EncryptionManager();

        // Admin
        Staff admin = new Staff();
        admin.fullName = encryptionManager.encrypt("Mark");
        admin.email = encryptionManager.encrypt("admin@hospital.com");
        admin.role = Staff.Role.ADMIN;
        admin.adminPin = encryptionManager.encrypt("1234"); // Encrypt the PIN
        db.staffDao().insert(admin);

        // Clinicians
        Staff clinician1 = new Staff();
        clinician1.fullName = encryptionManager.encrypt("Dr. Emily Carter");
        clinician1.email = encryptionManager.encrypt("emily.carter@hospital.com");
        clinician1.role = Staff.Role.CLINICIAN;
        db.staffDao().insert(clinician1);

        Staff clinician2 = new Staff();
        clinician2.fullName = encryptionManager.encrypt("Dr. Ben Richards");
        clinician2.email = encryptionManager.encrypt("ben.richards@hospital.com");
        clinician2.role = Staff.Role.CLINICIAN;
        db.staffDao().insert(clinician2);

        // Reception
        Staff reception = new Staff();
        reception.fullName = encryptionManager.encrypt("Sarah Jenkins");
        reception.email = encryptionManager.encrypt("reception@hospital.com");
        reception.role = Staff.Role.RECEPTION;
        db.staffDao().insert(reception);
        Log.d(TAG, "Staff seeding complete.");
    }

    private static void seedPatients(Context context, AppDatabase db) throws Exception {
        Log.d(TAG, "Seeding patients...");
        EncryptionManager encryptionManager = new EncryptionManager();

        // Patient for testing login
        Patient loginPatient = new Patient();
        loginPatient.nhsNumber = "1234567890"; // Known, unencrypted NHS number
        loginPatient.fullName = encryptionManager.encrypt("John Doe");
        loginPatient.dateOfBirth = encryptionManager.encrypt("1985-05-20");
        loginPatient.phone = encryptionManager.encrypt("07123456789");
        // Known, plain-text email for login test before it gets encrypted
        loginPatient.email = encryptionManager.encrypt("patient@test.com");
        loginPatient.createdAt = System.currentTimeMillis();
        loginPatient.updatedAt = System.currentTimeMillis();
        db.patientDao().insert(loginPatient);

        // Other dummy patients
        for (int i = 0; i < 5; i++) {
            Patient p = new Patient();
            p.nhsNumber = "987654321" + i;
            p.fullName = encryptionManager.encrypt("Patient Name " + i);
            p.dateOfBirth = encryptionManager.encrypt("1990-01-0" + (i + 1));
            p.phone = encryptionManager.encrypt("0798765432" + i);
            p.email = encryptionManager.encrypt("dummy" + i + "@test.com");
            p.createdAt = System.currentTimeMillis();
            p.updatedAt = System.currentTimeMillis();
            db.patientDao().insert(p);
        }
        Log.d(TAG, "Patient seeding complete.");
    }

    private static void seedAppointments(AppDatabase db) {
        Log.d(TAG, "Seeding appointments...");
        long now = System.currentTimeMillis();

        // Appointment for our test patient "John Doe"
        Appointment apt1 = new Appointment();
        apt1.patientNhsNumber = "1234567890"; // Belongs to John Doe
        apt1.startTime = now + 24 * 60 * 60 * 1000; // Tomorrow
        apt1.endTime = apt1.startTime + 30 * 60 * 1000; // 30 mins later
        apt1.clinicianId = 2; // Dr. Emily Carter
        apt1.clinicianName = "Dr. Emily Carter";
        apt1.clinic = "North Clinic";
        apt1.status = "BOOKED";
        db.appointmentDao().insert(apt1);

        // Another appointment
        Appointment apt2 = new Appointment();
        apt2.patientNhsNumber = "9876543210";
        apt2.startTime = now + 48 * 60 * 60 * 1000; // In two days
        apt2.endTime = apt2.startTime + 60 * 60 * 1000; // 1 hour later
        apt2.clinicianId = 3; // Dr. Ben Richards
        apt2.clinicianName = "Dr. Ben Richards";
        apt2.clinic = "South Clinic";
        apt2.status = "BOOKED";
        db.appointmentDao().insert(apt2);

        // Cancelled appointment
        Appointment apt3 = new Appointment();
        apt3.patientNhsNumber = "9876543211";
        apt3.startTime = now - 24 * 60 * 60 * 1000; // Yesterday
        apt3.endTime = apt3.startTime + 30 * 60 * 1000;
        apt3.clinicianId = 2; // Dr. Emily Carter
        apt3.clinicianName = "Dr. Emily Carter";
        apt3.clinic = "North Clinic";
        apt3.status = "CANCELLED";
        db.appointmentDao().insert(apt3);
        Log.d(TAG, "Appointment seeding complete.");
    }
}
