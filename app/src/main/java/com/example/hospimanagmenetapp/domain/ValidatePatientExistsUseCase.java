package com.example.hospimanagmenetapp.domain;

import android.content.Context;
import android.util.Log;

import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.data.repo.EhrRepository;
import com.example.hospimanagmenetapp.security.EncryptionManager;

import java.util.List;

public class ValidatePatientExistsUseCase {

    private static final String TAG = "ValidatePatientExists";
    private final EhrRepository ehrRepository;

    public ValidatePatientExistsUseCase(Context context){
        this.ehrRepository = new EhrRepository(context);
    }

    // Domain for validating whether a patient already exists within the database.
    public boolean execute(String plainTextNhsNumber) throws Exception {
        List<Patient> allPatients = ehrRepository.getAndCacheAllPatients();

        for (Patient patient : allPatients) {
            try {
                String decryptedNhs = new EncryptionManager().decrypt(patient.enPatientNhsNumber);
                if (plainTextNhsNumber.equals(decryptedNhs)) {
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt NHS number", e);
            }
        }
        return false;
    }
}
