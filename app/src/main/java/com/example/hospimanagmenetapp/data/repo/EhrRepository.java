package com.example.hospimanagmenetapp.data.repo;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.dao.ClinicalRecordDao;
import com.example.hospimanagmenetapp.data.dao.PatientDao;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.util.AppExecutors;
import com.example.hospimanagmenetapp.util.EncryptionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EhrRepository {
    private static final String TAG = "EhrRepository";
    private final PatientDao patientDao;
    private final ClinicalRecordDao clinicalRecordDao;
    private final EncryptionManager encryptionManager;

    public EhrRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.patientDao = db.patientDao();
        this.clinicalRecordDao = db.clinicalRecordDao();
        try {
            this.encryptionManager = new EncryptionManager();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EncryptionManager", e);
        }
    }

    public void getAllDecryptedPatients(Consumer<List<Patient>> callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Patient> encryptedPatients = patientDao.getAll();
            List<Patient> decryptedPatients = new ArrayList<>();
            for (Patient p : encryptedPatients) {
                try {
                    // Decrypt patient name for display in the Spinner
                    p.fullName = encryptionManager.decrypt(p.fullName);
                    decryptedPatients.add(p);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to decrypt patient name");
                }
            }
            // Post the result back to the UI thread
            AppExecutors.getInstance().mainThread().execute(() -> callback.accept(decryptedPatients));
        });
    }

    public void getClinicalRecord(String nhsNumber, Consumer<ClinicalRecord> callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            ClinicalRecord record = clinicalRecordDao.findByPatient(nhsNumber);
            // Post the result back to the UI thread
            AppExecutors.getInstance().mainThread().execute(() -> callback.accept(record));
        });
    }
}
