package com.example.hospimanagmenetapp.domain;

import android.content.Context;
import android.util.Log;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.util.EncryptionManager;

import java.util.List;

public class ValidatePatientExistsUseCase {

    private static final String TAG = "ValidatePatientExists";
    private final AppDatabase db;
    private final EncryptionManager encryptionManager;

    public ValidatePatientExistsUseCase(Context context) throws Exception {
        this.db = AppDatabase.getInstance(context);
        this.encryptionManager = new EncryptionManager();
    }

    /**
     * Checks if a patient with the given plaintext NHS number exists in the database.
     * This method handles the necessary decryption of stored NHS numbers.
     *
     * @param plainTextNhsNumber The decrypted NHS number to validate.
     * @return true if a patient with that NHS number exists, false otherwise.
     */
    public boolean execute(String plainTextNhsNumber) throws Exception {
        // 1. Fetch all patients from the database.
        List<Patient> allPatients = db.patientDao().getAll();

        // 2. Decrypt each patient's NHS number and compare it to the input.
        for (Patient patient : allPatients) {
            try {
                String decryptedNhs = encryptionManager.decrypt(patient.enPatientNhsNumber);
                if (plainTextNhsNumber.equals(decryptedNhs)) {
                    // 3. If a match is found, return true immediately.
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt NHS number for patient ID: " + patient.id, e);
                // Continue to the next patient, as this one might be corrupted.
            }
        }

        // 4. If the loop completes without finding a match, the patient does not exist.
        return false;
    }
}
