package com.example.hospimanagmenetapp.data.repo;


import android.util.Base64;
import android.util.Log;

import com.example.hospimanagmenetapp.data.dao.PatientDao;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.util.CryptoManager;

import java.util.ArrayList;
import java.util.List;

public class PatientRepository {

    private final PatientDao patientDao;

    public PatientRepository(PatientDao patientDao) {
        this.patientDao = patientDao;
    }

    // Encrypt sensitive fields before insert
    public long insert(Patient patient) {
        try {
            Patient encryptedPatient = new Patient();
            encryptedPatient.id = patient.id;
            encryptedPatient.nhsNumber = encrypt(patient.nhsNumber);
            encryptedPatient.fullName = encrypt(patient.fullName);
            encryptedPatient.dateOfBirth = patient.dateOfBirth; // Not encrypted
            encryptedPatient.phone = patient.phone;             // Not encrypted
            encryptedPatient.email = encrypt(patient.email);
            encryptedPatient.createdAt = patient.createdAt;
            encryptedPatient.updatedAt = patient.updatedAt;

            return patientDao.insert(encryptedPatient);
        } catch (Exception e) {
            Log.e("PatientRepository", "Encryption failed", e);
            return -1;
        }
    }

    // Decrypt sensitive fields after fetch
    public List<Patient> getAllDecryptedPatients() {
        List<Patient> decryptedList = new ArrayList<>();
        try {
            List<Patient> encryptedList = patientDao.getAllPatients(); // You’ll need to add this method to PatientDao
            for (Patient encrypted : encryptedList) {
                Patient decrypted = new Patient();
                decrypted.id = encrypted.id;
                decrypted.nhsNumber = decrypt(encrypted.nhsNumber);
                decrypted.fullName = decrypt(encrypted.fullName);
                decrypted.dateOfBirth = encrypted.dateOfBirth;
                decrypted.phone = encrypted.phone;
                decrypted.email = decrypt(encrypted.email);
                decrypted.createdAt = encrypted.createdAt;
                decrypted.updatedAt = encrypted.updatedAt;

                decryptedList.add(decrypted);
            }
        } catch (Exception e) {
            Log.e("PatientRepository", "Decryption failed", e);
        }
        return decryptedList;
    }

    // Helper methods
    private String encrypt(String plainText) throws Exception {
        return Base64.encodeToString(CryptoManager.encrypt(plainText), Base64.DEFAULT);
    }

    private String decrypt(String base64CipherText) throws Exception {
        return CryptoManager.decrypt(Base64.decode(base64CipherText, Base64.DEFAULT));
    }
}

