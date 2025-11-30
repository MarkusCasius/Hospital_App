package com.example.hospimanagmenetapp.feature.ehr.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.data.repo.EhrRepository;
import com.example.hospimanagmenetapp.security.RuntimeGuard;
import com.example.hospimanagmenetapp.security.auth.BiometricLoginCoordinator;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;
import com.example.hospimanagmenetapp.ui.BaseActivity;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PatientSummaryActivity extends BaseActivity {

    private EhrRepository ehrRepository;
    private Spinner spPatients;
    private TextView tvHeader;
    private EditText etProblems, etAllergies, etMedications;
    private List<Patient> patientList;
    private ClinicalRecord currentRecord;

    // Activity displays a patient summary, allowing users to input new details and save them.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (RuntimeGuard.isEnvironmentUnsafe()) {
            Toast.makeText(this, "Application cannot run in this environment.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        super.onCreate(savedInstanceState);

        boolean bypassRbac = getIntent().getBooleanExtra("bypassRbacCheck", false);

        if (!bypassRbac && !RbacPolicyEvaluator.canViewEhr(this)) {
            Toast.makeText(this, "Access Denied. You do not have permission to view this page.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        new BiometricLoginCoordinator().authenticate(this, new BiometricLoginCoordinator.Callback() {
            @Override
            public void onSuccess() {
                setContentView(R.layout.activity_patient_summary);
                initializeViewsAndData();
            }

            @Override
            public void onFailure(String reason) {
                setContentView(R.layout.activity_patient_summary);
                initializeViewsAndData();
//                runOnUiThread(() -> {
//                    Toast.makeText(PatientSummaryActivity.this, "Authentication required: " + reason, Toast.LENGTH_LONG).show();
//                    finish();
//                });
            }
        });
    }

    private void initializeViewsAndData() {
        ehrRepository = new EhrRepository(this);
        spPatients = findViewById(R.id.spPatients);
        tvHeader = findViewById(R.id.tvPatientHeader);
        etProblems = findViewById(R.id.etProblems);
        etAllergies = findViewById(R.id.etAllergies);
        etMedications = findViewById(R.id.etMedications);
        Button btnVitals = findViewById(R.id.btnRecordVitals);
        Button btnSave = findViewById(R.id.btnSave);

        loadAllPatients();

        spPatients.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Patient selectedPatient = patientList.get(position);
                loadClinicalRecord(selectedPatient.enPatientNhsNumber);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                clearClinicalData();
            }
        });

        btnVitals.setOnClickListener(v -> {
            if (spPatients.getSelectedItem() != null) {
                Patient selectedPatient = (Patient) patientList.get(spPatients.getSelectedItemPosition());
                Intent i = new Intent(this, VitalsActivity.class);
                i.putExtra("nhsNumber", selectedPatient.enPatientNhsNumber);
                startActivity(i);
            } else {
                Toast.makeText(this, "Please select a patient first.", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> {
            if (!RbacPolicyEvaluator.canEditEhr(this)) {
                Toast.makeText(this, "Access Denied. You do not have permission to edit records.", Toast.LENGTH_LONG).show();
                return;
            }
            saveClinicalRecord();
        });
    }
    private void loadAllPatients() {
        ehrRepository.getAllDecryptedPatients(patients -> {
            this.patientList = patients;
            List<String> patientNames = patients.stream()
                    .map(p -> p.fullName + " (NHS: " + p.enPatientNhsNumber + ")")
                    .collect(Collectors.toList());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    patientNames);

            spPatients.setAdapter(adapter);

            // If an NHS number was passed from another activity, like barcode scanner, pre-select that patient
            String initialNhs = getIntent().getStringExtra("nhsNumber");
            if (initialNhs != null) {
                for (int i = 0; i < patients.size(); i++) {
                    if (patients.get(i).enPatientNhsNumber.equals(initialNhs)) {
                        spPatients.setSelection(i);
                        break;
                    }
                }
            }
        });
    }

    private void loadClinicalRecord(String nhsNumber) {
        tvHeader.setText("Patient NHS: " + nhsNumber);
        ehrRepository.getClinicalRecord(nhsNumber, record -> {
            this.currentRecord = record;
            if (record != null) {
                etProblems.setText(record.problems != null ? record.problems : "");
                etAllergies.setText(record.allergies != null ? record.allergies : "");
                etMedications.setText(record.medications != null ? record.medications : "");
            } else {
                clearClinicalData();
                Toast.makeText(this, "No clinical record found for this patient.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveClinicalRecord() {
        if (spPatients.getSelectedItem() == null || patientList == null || patientList.isEmpty()) {
            Toast.makeText(this, "Please select a patient.", Toast.LENGTH_SHORT).show();
            return;
        }
        Patient selectedPatient = patientList.get(spPatients.getSelectedItemPosition());
        final String decryptedNhsNumber = selectedPatient.enPatientNhsNumber;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ClinicalRecord recordToSave;

                if (currentRecord != null) {

                    recordToSave = currentRecord;
                } else {

                    Patient patientFromDb = ehrRepository.findPatientByDecryptedNhs(decryptedNhsNumber);
                    if (patientFromDb == null) {
                        runOnUiThread(() -> Toast.makeText(this, "Error: Cannot find patient in database.", Toast.LENGTH_LONG).show());
                        return;
                    }

                    recordToSave = new ClinicalRecord();
                    recordToSave.enPatientNhs = patientFromDb.enPatientNhsNumber;
                }

                recordToSave.problems = etProblems.getText().toString();
                recordToSave.allergies = etAllergies.getText().toString();
                recordToSave.medications = etMedications.getText().toString();

                ehrRepository.updateClinicalRecord(recordToSave, updatedRecord -> {
                    if (updatedRecord != null) {
                        Toast.makeText(this, "Clinical record updated successfully.", Toast.LENGTH_SHORT).show();
                        loadClinicalRecord(decryptedNhsNumber);
                    } else {
                        Toast.makeText(this, "Failed to update clinical record.", Toast.LENGTH_SHORT).show();
                    }

                });
            } catch (Exception e) {
                Log.e("PatientSummary", "Failed to save clinical record", e);
                runOnUiThread(() -> Toast.makeText(this, "An unexpected error occurred.", Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void clearClinicalData() {
        this.currentRecord = null;
        tvHeader.setText("Patient Details");
        etProblems.setText("");
        etAllergies.setText("");
        etMedications.setText("");
    }


}