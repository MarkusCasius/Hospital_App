package com.example.hospimanagmenetapp.data.repo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.dao.ClinicalRecordDao;
import com.example.hospimanagmenetapp.data.dao.PatientDao;
import com.example.hospimanagmenetapp.data.dao.VitalsDao;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.data.entities.Vitals;
import com.example.hospimanagmenetapp.network.ApiClient;
import com.example.hospimanagmenetapp.network.dto.ClinicalRecordDto;
import com.example.hospimanagmenetapp.network.dto.VitalsDto;
import com.example.hospimanagmenetapp.util.EncryptionManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import retrofit2.Response;

public class EhrRepository {
    private static final String TAG = "EhrRepository";
    private final PatientDao patientDao;
    private final ClinicalRecordDao clinicalRecordDao;
    private final VitalsDao vitalsDao;
    private final EncryptionManager encryptionManager;
    private final ApiClient api;

    public EhrRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.patientDao = db.patientDao();
        this.clinicalRecordDao = db.clinicalRecordDao();
        this.vitalsDao = db.vitalsDao();
        this.api = new ApiClient(context);
        try {
            this.encryptionManager = new EncryptionManager();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EncryptionManager", e);
        }
    }

    public void getAllDecryptedPatients(Consumer<List<Patient>> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Patient> encryptedPatients = patientDao.getAll();
            List<Patient> decryptedPatients = null;
            try {
                decryptedPatients = EncryptionManager.decryptPatients(encryptedPatients);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<Patient> finalDecryptedPatients = decryptedPatients;
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(finalDecryptedPatients));
        });
    }

    public void getClinicalRecord(String nhsNumber, Consumer<ClinicalRecord> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Mock Network call
            try {
                Response<List<ClinicalRecordDto>> response = api.ehrApi().getRecord(nhsNumber).execute();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    ClinicalRecordDto dto = response.body().get(0);
                    ClinicalRecord networkRecord = map(dto);
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(networkRecord));
                } else {
                    Log.w(TAG, "Failed to fetch or found no clinical record from network for NHS: " + nhsNumber);
                    // new Handler(Looper.getMainLooper()).post(()-> callback.accept(null));
                }
            } catch (IOException e) {
                Log.e(TAG, "Network error fetching clinical record for NHS: " + nhsNumber, e);
                // new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
            }
            // Return from Truth (database)
            Log.d(TAG, "Fetching clinical record from local database.");
            // Fetch all records from the database
            List<ClinicalRecord> allRecords = clinicalRecordDao.getAllRecords();
            ClinicalRecord foundRecord = null;

            // Iterate, decrypt, and compare
            for (ClinicalRecord record : allRecords) {
                try {
                    String decryptedNhs = encryptionManager.decrypt(record.enPatientNhs);
                    if (nhsNumber.equals(decryptedNhs)) {
                        foundRecord = record; // Record Found
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to decrypt NHS number for record id: " + record.id, e);
                    // Continue to the next record
                }
            }

            // Return the found record (or null) to the UI thread
            final ClinicalRecord finalFoundRecord = foundRecord;
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(finalFoundRecord));
        });
    }

    public void updateClinicalRecord(ClinicalRecord record, Consumer<ClinicalRecord> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ClinicalRecordDto requestDto = mapToDto(record);
                Response<ClinicalRecordDto> response = api.ehrApi().updateorcreateRecord(requestDto).execute();
                if (response.isSuccessful() && response.body() != null) {
                    ClinicalRecordDto responseDto = response.body();
                    ClinicalRecord updatedRecord = map(responseDto);
                    clinicalRecordDao.upsert(updatedRecord);
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(updatedRecord));
                } else {
                    Log.w(TAG, "Failed to update clinical record from network for NHS: " + record.enPatientNhs);
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
                }
            } catch (IOException e) {
                Log.e(TAG, "Network error updating clinical record for NHS: " + record.enPatientNhs, e);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
            }
        });
    }

    public void saveVitals(Vitals vitals, Consumer<Boolean> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // VitalsDto requestDto = mapToDto(vitals);
                // Response<ClinicalRecordDto> response = api.ehrApi().uploadVitals(requestDto).execute();
                vitalsDao.insert(vitals);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(true));
            } catch (Exception e) {
                Log.e(TAG, "Failed to save vitals locally.", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
            }
        });
    }

    public List<Vitals> getUnsyncedVitals() {
        return vitalsDao.getPending();
    }

    public boolean syncVitals(Vitals vitals) {
        try {
            VitalsDto dto = mapToDto(vitals);
            Response<Void> response = api.ehrApi().uploadVitals(dto).execute();
            if (response.isSuccessful()) {
                vitalsDao.markSynced(vitals.id);
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to sync vitals for patient " + vitals.enPatientNhsNumber, e);
        }
        return false;
    }

    public int getVitalsCountForPatient(String nhsNumber) {
        return vitalsDao.getVitalsCountForPatient(nhsNumber);
    }

    public List<Vitals> getVitalsForPatientPaged(String nhsNumber, int pageSize, int offset) {
        return vitalsDao.getVitalsForPatientPaged(nhsNumber, pageSize, offset);
    }

    public List<Vitals> getAllVitalsForPatient(String decryptedNhsNumber) {
        // Fetch all vitals from the database.
        List<Vitals> allVitals = vitalsDao.getAllVitals();
        List<Vitals> patientVitals = new ArrayList<>();

        // Iterate, decrypt the stored NHS number, and compare.
        for (Vitals vital : allVitals) {
            try {
                String storedDecryptedNhs = encryptionManager.decrypt(vital.enPatientNhsNumber);
                // If it matches, add it to our list.
                if (decryptedNhsNumber.equals(storedDecryptedNhs)) {
                    patientVitals.add(vital);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt NHS number for vital record id: " + vital.id, e);
            }
        }
        // Return the fully filtered list of vitals for the specific patient.
        return patientVitals;
    }

    private ClinicalRecord map(ClinicalRecordDto dto) {
        ClinicalRecord record = new ClinicalRecord();
        ClinicalRecord existing = clinicalRecordDao.findByPatient(dto.enPatientNhs);
        if (existing != null) {
            record.id = existing.id;
        }
        record.enPatientNhs = dto.enPatientNhs;
        record.allergies = dto.allergies;
        record.medications = dto.medications;
        record.problems = dto.problems;
        record.updatedAt = dto.updatedAt;
        return record;
    }

    private ClinicalRecordDto mapToDto(ClinicalRecord record) {
        ClinicalRecordDto dto = new ClinicalRecordDto();
        dto.id = record.id;
        dto.enPatientNhs = record.enPatientNhs;
        dto.allergies = record.allergies;
        dto.medications = record.medications;
        dto.problems = record.problems;
        dto.updatedAt = System.currentTimeMillis();
        return dto;
    }

    private VitalsDto mapToDto(Vitals vitals) {
        VitalsDto dto = new VitalsDto();
        dto.enPatientNhs = vitals.enPatientNhsNumber;
        dto.temperature = vitals.temperature;
        dto.heartRate = vitals.heartRate;
        dto.systolic = vitals.systolic;
        dto.diastolic = vitals.diastolic;
        dto.timestamp = vitals.timestamp;
        return dto;
    }
}
