package com.example.hospimanagmenetapp.feature.ehr.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;

import java.util.List;
import java.util.stream.Collectors;

public class PatientSummaryActivity extends AppCompatActivity {

    private EhrRepository ehrRepository;
    private Spinner spPatients;
    private TextView tvHeader;
    private EditText etProblems, etAllergies, etMedications;
    private Button btnVitals, btnSave;
    private List<Patient> patientList;
    private ClinicalRecord currentRecord;


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
        etProblems = findViewById(R.id.etProblems);
        etAllergies = findViewById(R.id.etAllergies);
        etMedications = findViewById(R.id.etMedications);
        btnVitals = findViewById(R.id.btnRecordVitals);
        btnSave = findViewById(R.id.btnSave);

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
                    .map(p -> p.fullName + " (NHS: " + p.nhsNumber + ")")
                    .collect(Collectors.toList());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    patientNames);

            spPatients.setAdapter(adapter);

            // If an NHS number was passed from another activity, like barcode scanner, pre-select that patient
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
        if (currentRecord == null) {
            Toast.makeText(this, "No record loaded to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentRecord.problems = etProblems.getText().toString();
        currentRecord.allergies = etAllergies.getText().toString();
        currentRecord.medications = etMedications.getText().toString();

        ehrRepository.updateClinicalRecord(currentRecord, updatedRecord -> {
            if (updatedRecord != null) {
                Toast.makeText(this, "Clinical record updated successfully.", Toast.LENGTH_SHORT).show();
                // Optionally reload the data to confirm it's saved
                loadClinicalRecord(updatedRecord.enPatientNhs);
            } else {
                Toast.makeText(this, "Failed to update clinical record.", Toast.LENGTH_SHORT).show();
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