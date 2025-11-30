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
import com.example.hospimanagmenetapp.network.dto.PatientDto;
import com.example.hospimanagmenetapp.network.dto.VitalsDto;
import com.example.hospimanagmenetapp.security.EncryptionManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import retrofit2.Response;

// A repository for handling all EHR (Electronic Health Record) data interactions,
// including patients, clinical records, and vitals.
// It coordinates between the network API and local database.

public class EhrRepository {
    private static final String TAG = "EhrRepository";
    private final PatientDao patientDao;
    private final ClinicalRecordDao clinicalRecordDao;
    private final VitalsDao vitalsDao;
    private final EncryptionManager encryptionManager;
    private final ApiClient api;

    // Constructs the EhrRepository, initializing all necessary DAOs and services.
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

    // -- Patient Methods --

    // Refreshes the local patient cache by fetching all patients from the network API.
    public void refreshPatientCache() {
        try {
            // Mock network
            Log.d(TAG, "Refreshing patient cache from network.");
            Response<List<PatientDto>> response = api.ehrApi().getAllPatients().execute();
            if (response.isSuccessful() && response.body() != null) {
                // In a real app, this would use an upsert operation.
                for (PatientDto dto : response.body()) {
                    Patient patient = new Patient();
                    patient.enPatientNhsNumber = dto.enPatientNhsNumber;
                    patient.fullName = dto.fullName;
                    patient.dateOfBirth = dto.dateOfBirth;
                    patient.email = dto.email;
                    patient.phone = dto.phone;
                    patient.createdAt = dto.createdAt;
                    patient.updatedAt = dto.updatedAt;
                    patientDao.insert(patient);
                }
                Log.d(TAG, "Successfully cached " + response.body().size() + " patients from network.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to sync patients from network, using local cache.", e);
        }
    }

    // Gets all patients, ensuring the local cache is refreshed from the network first.
    public List<Patient> getAndCacheAllPatients() {
        refreshPatientCache();
        return patientDao.getAll();
    }

    // Decrypts a list of encrypted Patient objects.
    public List<Patient> getDecryptedPatients(List<Patient> encryptedPatients) throws Exception {
        return EncryptionManager.decryptPatients(encryptedPatients);
    }

    // Asynchronously gets all patients, decrypts them, and returns them via a callback.
    public void getAllDecryptedPatients(Consumer<List<Patient>> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Patient> encryptedPatients = getAndCacheAllPatients();
            try {
                List<Patient> decryptedPatients = getDecryptedPatients(encryptedPatients);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(decryptedPatients));
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt patients", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(new ArrayList<>()));
            }
        });
    }

    // Saves a new patient to the network and, upon success, to the local database.
    public void savePatient(Patient patient) throws Exception {
        // Encrypt the patient data before sending or saving
        Patient encryptedPatient = new Patient();
        encryptedPatient.enPatientNhsNumber = encryptionManager.encrypt(patient.enPatientNhsNumber);
        encryptedPatient.fullName = encryptionManager.encrypt(patient.fullName);
        encryptedPatient.dateOfBirth = encryptionManager.encrypt(patient.dateOfBirth);
        encryptedPatient.phone = encryptionManager.encrypt(patient.phone);
        encryptedPatient.email = encryptionManager.encrypt(patient.email);
        encryptedPatient.createdAt = patient.createdAt;
        encryptedPatient.updatedAt = patient.updatedAt;

        // Map the encrypted Entity to a DTO for the network call
        PatientDto dto = mapToDto(encryptedPatient);

        // Network call
        Log.d(TAG, "Saving patient to network");
        Response<Void> response = api.ehrApi().savePatient(dto).execute();

        if (response.isSuccessful()) {
            Log.d(TAG, "Network save successful. Saving to local database.");
            patientDao.insert(encryptedPatient);
        } else {
            // If the network fails, throw an exception to be handled by the UI
            throw new IOException("API Error: " + response.code() + " " + response.message());
        }
    }

    // Asynchronously gets a patient's clinical record, refreshing from the network first.
    public void getClinicalRecord(String nhsNumber, Consumer<ClinicalRecord> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Mock Network call
            try {
                Log.d(TAG, "Refreshing clinical record from network");
                Response<List<ClinicalRecordDto>> response = api.ehrApi().getRecord(nhsNumber).execute();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    ClinicalRecord networkRecord = map(response.body().get(0));
                    clinicalRecordDao.upsert(networkRecord); // Cache the result
                    Log.d(TAG, "Successfully cached clinical record from network.");
                }
            } catch (IOException e) {
                Log.w(TAG, "Network error fetching clinical record, using local data.", e);
            }
            // Return from Truth (database)
            Log.d(TAG, "Fetching clinical record from local database.");
            List<ClinicalRecord> allRecords = clinicalRecordDao.getAllRecords();
            ClinicalRecord foundRecord = null;
            for (ClinicalRecord record : allRecords) {
                try {
                    String decryptedNhs = encryptionManager.decrypt(record.enPatientNhs);
                    if (nhsNumber.equals(decryptedNhs)) {
                        foundRecord = record;
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to decrypt NHS number for record id: " + record.id, e);
                }
            }

            // Return the found record (or null) to the UI thread
            final ClinicalRecord finalFoundRecord = foundRecord;
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(finalFoundRecord));
        });
    }

    // Asynchronously creates or updates a clinical record on the network and caches the result locally.
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

    // Finds a patient by their decrypted NHS number by searching the local database.
    public Patient findPatientByDecryptedNhs(String decryptedNhsNumber) {
        List<Patient> allPatients = getAndCacheAllPatients();
        for (Patient patient : allPatients) {
            try {
                String decryptedNhs = encryptionManager.decrypt(patient.enPatientNhsNumber);
                if (decryptedNhsNumber.equals(decryptedNhs)) {
                    return patient; // Return the full patient object (which has the encrypted NHS)
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt NHS number for patient ID: " + patient.id, e);
            }
        }
        return null; // Return null if no patient is found
    }

    // -- Vitals Methods --

    // Refreshes the local cache of vitals for a specific patient from the network.
    private void refreshVitalsCache(String decryptedNhsNumber) {
        try {
            Log.d(TAG, "Refreshing vitals from network");
            Response<List<VitalsDto>> response = api.ehrApi().getVitals(decryptedNhsNumber).execute();
            if (response.isSuccessful() && response.body() != null) {
                for (VitalsDto dto : response.body()) {
                    Vitals vitals = new Vitals();
                    vitals.enPatientNhsNumber = encryptionManager.encrypt(dto.enPatientNhs);
                    vitals.temperature = dto.temperature;
                    vitals.heartRate = dto.heartRate;
                    vitals.systolic = dto.systolic;
                    vitals.diastolic = dto.diastolic;
                    vitals.timestamp = dto.timestamp;
                    vitals.synced = true; // Data from network is always considered synced
                    vitalsDao.insert(vitals);
                }
                Log.d(TAG, "Successfully cached " + response.body().size() + " vitals from network.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Network error fetching vitals, using local data.", e);
        }
    }

    // Saves a vitals record to the local database for later synchronization.
    public void saveVitals(Vitals vitals, Consumer<Boolean> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                vitalsDao.insert(vitals);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(true));
            } catch (Exception e) {
                Log.e(TAG, "Failed to save vitals locally.", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
            }
        });
    }

    // Gets all vitals for a specific patient, refreshing from the network first.
    public List<Vitals> getAllVitalsForPatient(String decryptedNhsNumber) {
        refreshVitalsCache(decryptedNhsNumber);

        List<Vitals> allVitals = vitalsDao.getAllVitals();
        List<Vitals> patientVitals = new ArrayList<>();

        for (Vitals vital : allVitals) {
            try {
                String storedDecryptedNhs = encryptionManager.decrypt(vital.enPatientNhsNumber);
                if (decryptedNhsNumber.equals(storedDecryptedNhs)) {
                    patientVitals.add(vital);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt NHS number for vital record id: " + vital.id, e);
            }
        }
        return patientVitals;
    }

    // Attempts to sync a single vitals record to the network.
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

    public List<Vitals> getUnsyncedVitals() {
        return vitalsDao.getPending();
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

    private PatientDto mapToDto(Patient patient) {
        PatientDto dto = new PatientDto();
        dto.enPatientNhsNumber = patient.enPatientNhsNumber;
        dto.fullName = patient.fullName;
        dto.dateOfBirth = patient.dateOfBirth;
        dto.email = patient.email;
        dto.phone = patient.phone;
        dto.createdAt = patient.createdAt;
        dto.updatedAt = patient.updatedAt;
        return dto;
    }
}
