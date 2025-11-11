package com.example.hospimanagmenetapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.util.EncryptionManager;
import com.example.hospimanagmenetapp.util.SessionManager;

import java.util.concurrent.Executors;

public class PatientLoginActivity extends AppCompatActivity {

    private static final String TAG = "PatientLoginActivity"; // Tag for filtering logs
    private EditText NhsNumber;
    private EditText Email;
    private Button btnLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_login);

        NhsNumber = findViewById(R.id.etPatientNhsNumber);
        Email = findViewById(R.id.etPatientEmail);
        btnLogin = findViewById(R.id.btnPatientLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String nhsNumber = NhsNumber.getText().toString().trim();
        String emailInput = Email.getText().toString().trim();

        Log.d(TAG, "Attempting login with NHS: " + nhsNumber + " and Email: " + emailInput);

        if (nhsNumber.isEmpty() || emailInput.isEmpty()) {
            Toast.makeText(this, "NHS Number and Email are required.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Login failed: NHS number or email was empty.");
            return;
        }

        // Perform database lookup and decryption on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Log.d(TAG, "Background thread started for DB lookup.");
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                // 1. Fetch the patient record using only the unencrypted NHS number.
                Patient patient = db.patientDao().findByNhs(nhsNumber);

                boolean loginSuccess = false;
                if (patient != null) {
                    Log.d(TAG, "Patient found for NHS number: " + nhsNumber);
                    // 2. Decrypt the email from the database record.
                    EncryptionManager encryptionManager = new EncryptionManager();
                    String storedEncryptedEmail = patient.email;
                    Log.d(TAG, "Encrypted email from DB: " + storedEncryptedEmail);

                    String decryptedEmail = encryptionManager.decrypt(storedEncryptedEmail);
                    Log.d(TAG, "Decrypted email: " + decryptedEmail);

                    // 3. Compare the decrypted email with the user's input.
                    if (emailInput.equals(decryptedEmail)) {
                        loginSuccess = true;
                        Log.i(TAG, "Login successful: Email match for NHS " + nhsNumber);
                    } else {
                        Log.w(TAG, "Login failed: Email mismatch. Input: " + emailInput + ", Decrypted: " + decryptedEmail);
                    }
                } else {
                    Log.w(TAG, "Login failed: No patient found for NHS number: " + nhsNumber);
                }

                // Safely update the UI on the main thread
                final boolean finalLoginSuccess = loginSuccess;
                final Patient finalPatient = patient; // To be used on the main thread
                runOnUiThread(() -> {
                    if (finalLoginSuccess) {
                        // On success, save session and navigate.
                        SessionManager.setCurrentUser(this, "PATIENT", finalPatient.nhsNumber);
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(PatientLoginActivity.this, AppointmentActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // On failure (no patient or email mismatch), show an error.
                        Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                // This will catch any error during DB access or decryption.
                Log.e(TAG, "An exception occurred during the login process.", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "An error occurred during login.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
