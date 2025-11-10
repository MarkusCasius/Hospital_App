package com.example.hospimanagmenetapp.feature.ehr.ui;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import java.util.concurrent.Executors;

public class PatientSummaryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_summary);

        TextView tvHeader = findViewById(R.id.tvPatientHeader);
        TextView tvProblems = findViewById(R.id.tvProblems);
        TextView tvAllergies = findViewById(R.id.tvAllergies);
        TextView tvMedications = findViewById(R.id.tvMedications);
        Button btnVitals = findViewById(R.id.btnRecordVitals);

        String nhs = getIntent().getStringExtra("nhsNumber");
        tvHeader.setText("Patient NHS: " + nhs);

        Executors.newSingleThreadExecutor().execute(() -> {
            ClinicalRecord record = AppDatabase.getInstance(getApplicationContext())
                    .clinicalRecordDao().findByPatient(nhs);
            runOnUiThread(() -> {
                if (record != null) {
                    tvProblems.setText("Problems: " + record.problems);
                    tvAllergies.setText("Allergies: " + record.allergies);
                    tvMedications.setText("Medications: " + record.medications);
                } else {
                    tvProblems.setText("No clinical record found.");
                }
            });
        });

        btnVitals.setOnClickListener(v -> {
            Intent i = new Intent(this, VitalsActivity.class);
            i.putExtra("nhsNumber", nhs);
            startActivity(i);
        });
    }
}

