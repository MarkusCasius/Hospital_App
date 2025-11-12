package com.example.hospimanagmenetapp.ui; // UI layer package for Activities

import androidx.appcompat.app.AppCompatActivity; // Base class for Activities with AppCompat support

import android.app.DatePickerDialog;
import android.os.Bundle;        // Lifecycle state bundle
import android.text.TextUtils;   // Utility for simple string emptiness checks
import android.widget.Button;    // UI widget: Button
import android.widget.EditText;  // UI widget: text input
import android.widget.Toast;     // Lightweight user notifications

import com.example.hospimanagmenetapp.R;                    // Resource IDs (layouts, strings, etc.)
import com.example.hospimanagmenetapp.data.AppDatabase;     // Room database singleton
import com.example.hospimanagmenetapp.data.entities.Patient; // Entity to persist
import com.example.hospimanagmenetapp.util.DatePickerUtils;
import com.example.hospimanagmenetapp.util.EncryptionManager;
import com.example.hospimanagmenetapp.util.ValidationUtils; // NHS number validator

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors; // For running DB work off the main thread

public class PatientRegistrationActivity extends AppCompatActivity { // Screen to capture and save a patient

    private EditText etNhs, etFullName, etDob, etPhone, etEmail; // Form inputs
    private Button btnSave;                                      // Save action
    private final Calendar dobCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Activity creation lifecycle
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_registration); // Inflate the registration form layout

        // Bind views to fields
        etNhs = findViewById(R.id.etNhs);
        etFullName = findViewById(R.id.etFullName);
        etDob = findViewById(R.id.etDob);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        btnSave = findViewById(R.id.btnSavePatient);
        etDob.setOnClickListener(v -> DatePickerUtils.showDatePickerDialog(this, dobCalendar, () -> {
            SimpleDateFormat sdf = new SimpleDateFormat(DatePickerUtils.APP_DATE_FORMAT, Locale.UK);
            etDob.setText(sdf.format(dobCalendar.getTime()));
        }));

        btnSave.setOnClickListener(v -> savePatient()); // When tapped, validate and persist the patient
    }

    // Validate inputs and insert the patient into Room on a background thread
    private void savePatient() {
        // Read and trim user input
        String nhs = etNhs.getText().toString().trim();
        String name = etFullName.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        // Basic required-field validation
        if (TextUtils.isEmpty(nhs) || TextUtils.isEmpty(name) || TextUtils.isEmpty(dob)) {
            Toast.makeText(this, "NHS number, name, and DOB are required.", Toast.LENGTH_SHORT).show();
            return; // Stop here; user must complete the required fields
        }

        // Validate the NHS number using Mod 11 rules
        if (!ValidationUtils.validateNhsNumber(nhs)) {
            Toast.makeText(this, "Invalid NHS number.", Toast.LENGTH_SHORT).show();
            return; // Do not proceed with invalid identifiers
        }

        // Run database I/O off the main thread to keep the UI responsive
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                EncryptionManager encryptionManager = new EncryptionManager();

                // Encrypt the sensitive fields
                String encryptedName = encryptionManager.encrypt(name);
                String encryptedDob = encryptionManager.encrypt(dob);
                String encryptedPhone = encryptionManager.encrypt(phone);
                String encryptedEmail = encryptionManager.encrypt(email);

                AppDatabase db = AppDatabase.getInstance(getApplicationContext()); // Get the Room singleton

                // Enforce uniqueness by NHS number before inserting
                if (db.patientDao().countByNhs(nhs) > 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Patient with this NHS number already exists.", Toast.LENGTH_SHORT).show());
                    return; // Abort insert; duplicate detected
                }

                // Map form inputs to a new Patient entity
                Patient p = new Patient();
                p.nhsNumber = nhs;
                p.fullName = encryptedName;
                p.dateOfBirth = encryptedDob; // Consider normalising/validating format upstream
                p.phone = encryptedPhone;
                p.email = encryptedEmail;
                long now = System.currentTimeMillis(); // Timestamp fields in epoch millis
                p.createdAt = now;
                p.updatedAt = now;

                db.patientDao().insert(p); // Persist to the local database

                // Notify success and close the screen
                runOnUiThread(() -> {
                    Toast.makeText(this, "Patient saved.", Toast.LENGTH_SHORT).show();
                    finish(); // Return to the previous screen
                });
            } catch (Exception e) {
                // Generic error path (e.g., SQLite constraint, I/O issues)
                runOnUiThread(() ->
                        Toast.makeText(this, "Error saving patient.", Toast.LENGTH_SHORT).show());
            }
        });
    }
}