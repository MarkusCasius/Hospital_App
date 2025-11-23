package com.example.hospimanagmenetapp.ui; // UI layer package for Activities

import androidx.appcompat.app.AppCompatActivity; // Base class for Activities with AppCompat support

import android.os.Bundle;        // Lifecycle state bundle
import android.text.TextUtils;   // Utility for simple string emptiness checks
import android.widget.Button;    // UI widget: Button
import android.widget.EditText;  // UI widget: text input
import android.widget.Toast;     // Lightweight user notifications

import com.example.hospimanagmenetapp.R;                    // Resource IDs (layouts, strings, etc.)
import com.example.hospimanagmenetapp.data.entities.Patient; // Entity to persist
import com.example.hospimanagmenetapp.data.repo.EhrRepository;
import com.example.hospimanagmenetapp.domain.ValidatePatientExistsUseCase;
import com.example.hospimanagmenetapp.security.RuntimeGuard;
import com.example.hospimanagmenetapp.util.DatePickerUtils;
import com.example.hospimanagmenetapp.security.EncryptionManager;
import com.example.hospimanagmenetapp.util.ValidationUtils; // NHS number validator

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors; // For running DB work off the main thread

public class PatientRegistrationActivity extends AppCompatActivity { // Screen to capture and save a patient

    // Activity for saving patients to the database, with validation to ensure that it is a legal
    // operation before posting it to the database.

    private EhrRepository ehrRepository;
    private EditText etNhs, etFullName, etDob, etPhone, etEmail; // Form inputs
    private Button btnSave;                                      // Save action
    private final Calendar dobCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Activity creation lifecycle
        super.onCreate(savedInstanceState);

        if (RuntimeGuard.isEnvironmentUnsafe()) {
            Toast.makeText(this, "Application cannot run in this environment.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_patient_registration); // Inflate the registration form layout
        ehrRepository = new EhrRepository(this);

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

        // Run database
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                EncryptionManager encryptionManager = new EncryptionManager();
                if (new ValidatePatientExistsUseCase(this).execute(nhs)) {
                    runOnUiThread(() -> Toast.makeText(this, "Patient with this NHS number already exists.", Toast.LENGTH_SHORT).show());
                    return;
                }

                Patient p = new Patient();
                p.enPatientNhsNumber = nhs; // Pass plaintext NHS
                p.fullName = name;
                p.dateOfBirth = dob;
                p.phone = phone;
                p.email = email;
                long now = System.currentTimeMillis();
                p.createdAt = now;
                p.updatedAt = now;

                ehrRepository.savePatient(p);

                // Notify success and close the screen
                runOnUiThread(() -> {
                    Toast.makeText(this, "Patient saved.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error saving patient.", Toast.LENGTH_SHORT).show());
            }
        });
    }
}