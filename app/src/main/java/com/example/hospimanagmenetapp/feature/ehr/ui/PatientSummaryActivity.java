package com.example.hospimanagmenetapp.feature.ehr.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.data.repo.EhrRepository;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PatientSummaryActivity extends AppCompatActivity {

    private EhrRepository ehrRepository;
    private Spinner spPatients;
    private TextView tvHeader, tvProblems, tvAllergies, tvMedications;
    private Button btnVitals;
    private List<Patient> patientList;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_summary);

        if (!RbacPolicyEvaluator.canViewEhr(this)) {
            Toast.makeText(this, "Access Denied. You do not have permission to view this page.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ehrRepository = new EhrRepository(this);
        spPatients = findViewById(R.id.spPatients);
        tvHeader = findViewById(R.id.tvPatientHeader);
        tvProblems = findViewById(R.id.tvProblems);
        tvAllergies = findViewById(R.id.tvAllergies);
        tvMedications = findViewById(R.id.tvMedications);
        btnVitals = findViewById(R.id.btnRecordVitals);

        loadAllPatients();

        spPatients.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Patient selectedPatient = patientList.get(position);
                loadClinicalRecord(selectedPatient.nhsNumber);
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
                i.putExtra("nhsNumber", selectedPatient.nhsNumber);
                startActivity(i);
            } else {
                Toast.makeText(this, "Please select a patient first.", Toast.LENGTH_SHORT).show();
            }
        });
//        String nhs = getIntent().getStringExtra("nhsNumber");
//        tvHeader.setText("Patient NHS: " + nhs);
//
//        Executors.newSingleThreadExecutor().execute(() -> {
//            ClinicalRecord record = AppDatabase.getInstance(getApplicationContext())
//                    .clinicalRecordDao().findByPatient(nhs);
//            runOnUiThread(() -> {
//                if (record != null) {
//                    tvProblems.setText("Problems: " + record.problems);
//                    tvAllergies.setText("Allergies: " + record.allergies);
//                    tvMedications.setText("Medications: " + record.medications);
//                } else {
//                    tvProblems.setText("No clinical record found.");
//                }
//            });
//        });
//
//        btnVitals.setOnClickListener(v -> {
//            Intent i = new Intent(this, VitalsActivity.class);
//            i.putExtra("nhsNumber", nhs);
//            startActivity(i);
//        });
    }
    private void loadAllPatients() {
        ehrRepository.getAllDecryptedPatients(patients -> {
            this.patientList = patients;
            List<String> patientNames = patients.stream()
                    .map(p -> p.fullName + " (NHS: " + p.nhsNumber + ")")
                    .collect(Collectors.toList());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    patientNames);

            spPatients.setAdapter(adapter);

            // If an NHS number was passed from another activity, pre-select that patient
            String initialNhs = getIntent().getStringExtra("nhsNumber");
            if (initialNhs != null) {
                for (int i = 0; i < patients.size(); i++) {
                    if (patients.get(i).nhsNumber.equals(initialNhs)) {
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
            if (record != null) {
                tvProblems.setText("Problems: " + (record.problems != null ? record.problems : "N/A"));
                tvAllergies.setText("Allergies: " + (record.allergies != null ? record.allergies : "N/A"));
                tvMedications.setText("Medications: " + (record.medications != null ? record.medications : "N/A"));
            } else {
                clearClinicalData();
                tvProblems.setText("No clinical record found for this patient.");
            }
        });
    }

    private void clearClinicalData() {
        tvProblems.setText("Problems: ");
        tvAllergies.setText("Allergies: ");
        tvMedications.setText("Medications: ");
    }


}

