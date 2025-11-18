package com.example.hospimanagmenetapp.feature.ehr.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LoadState;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.Vitals;
import com.example.hospimanagmenetapp.data.repo.EhrRepository;
import com.example.hospimanagmenetapp.feature.ehr.ui.adapters.VitalsAdapter;

import java.util.List;
import java.util.concurrent.Executors;

public class VitalsActivity extends AppCompatActivity {


    private EhrRepository ehrRepository;
    private String patientNhsNumber;
    private EditText etTemperature, etHeartRate, etSystolic, etDiastolic;
    private VitalsAdapter vitalsAdapter;
    private RecyclerView rvVitalsHistory;
    private Button btnPreviousPage, btnNextPage;
    private TextView tvPageInfo;

    private int currentPage = 1;
    private int pageSize = 5; // A default page size
    private int totalItemCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vitals);
        ehrRepository = new EhrRepository(this);
        patientNhsNumber = getIntent().getStringExtra("nhsNumber");

        setupViews();

        rvVitalsHistory.post(() -> {
            pageSize = calculatePageSize();
            Log.d("VitalsActivity", "Calculated page size: " + pageSize);
            loadPage(); // Initial data load
        });
    }

    private void setupViews() {
        TextView tvHeader = findViewById(R.id.tvVitalsHeader);
        tvHeader.setText("Record Vitals for NHS: " + patientNhsNumber);

        etTemperature = findViewById(R.id.etTemperature);
        etHeartRate = findViewById(R.id.etHeartRate);
        etSystolic = findViewById(R.id.etSystolic);
        etDiastolic = findViewById(R.id.etDiastolic);
        tvPageInfo = findViewById(R.id.tvPageInfo);

        Button btnSaveVitals = findViewById(R.id.btnSaveVitals);
        btnSaveVitals.setOnClickListener(v -> saveVitals());

        rvVitalsHistory = findViewById(R.id.rvVitalsHistory);
        rvVitalsHistory.setLayoutManager(new LinearLayoutManager(this));
        vitalsAdapter = new VitalsAdapter(); // This is now a ListAdapter
        rvVitalsHistory.setAdapter(vitalsAdapter);

        btnPreviousPage = findViewById(R.id.btnPreviousPage);
        btnNextPage = findViewById(R.id.btnNextPage);

        btnNextPage.setOnClickListener(v -> {
            currentPage++;
            loadPage();
        });

        btnPreviousPage.setOnClickListener(v -> {
            currentPage--;
            loadPage();
        });
    }
    private void loadPage() {
        // Use AsyncTask for background database query
        Executors.newSingleThreadExecutor().execute(() -> {
            // First, get the total count of vitals for the patient
            totalItemCount = ehrRepository.getVitalsCountForPatient(patientNhsNumber);

            // Calculate total pages, ensuring at least 1 page
            int totalPages = (int) Math.ceil((double) totalItemCount / pageSize);
            if (totalPages == 0) totalPages = 1;

            // Clamp current page to be within valid range
            if (currentPage > totalPages) currentPage = totalPages;
            if (currentPage < 1) currentPage = 1;

            // Calculate the offset for the database query
            int offset = (currentPage - 1) * pageSize;
            List<Vitals> vitalsForPage = ehrRepository.getVitalsForPatientPaged(patientNhsNumber, pageSize, offset);

            // Update UI on the main thread
            int finalTotalPages = totalPages;
            runOnUiThread(() -> {
                vitalsAdapter.submitList(vitalsForPage);
                String pageInfo = "Page " + currentPage + " of " + finalTotalPages;
                tvPageInfo.setText(pageInfo);

                // Enable/disable buttons based on the current page
                btnPreviousPage.setEnabled(currentPage > 1);
                btnNextPage.setEnabled(currentPage < finalTotalPages);
            });
        });
    }

    private int calculatePageSize() {
        // Get the height of a single list item.
        View listItem = getLayoutInflater().inflate(R.layout.vitals_list_item, rvVitalsHistory, false);
        listItem.measure(
                View.MeasureSpec.makeMeasureSpec(rvVitalsHistory.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int itemHeight = listItem.getMeasuredHeight();

        if (itemHeight <= 0) return 10; // Fallback

        int recyclerViewHeight = rvVitalsHistory.getHeight();
        if(recyclerViewHeight <= 0) return 10; // Fallback

        return (int) Math.floor((double) recyclerViewHeight / itemHeight);
    }

    private void saveVitals() {
        try {
            Vitals vitals = new Vitals();
            vitals.enPatientNhs = patientNhsNumber;
            vitals.temperature = Float.parseFloat(etTemperature.getText().toString());
            vitals.heartRate = Integer.parseInt(etHeartRate.getText().toString());
            vitals.systolic = Integer.parseInt(etSystolic.getText().toString());
            vitals.diastolic = Integer.parseInt(etDiastolic.getText().toString());
            vitals.timestamp = System.currentTimeMillis();
            vitals.synced = false;

            ehrRepository.saveVitals(vitals, success -> {
                if (success) {
                    Toast.makeText(this, "Vitals saved locally.", Toast.LENGTH_SHORT).show();
                    clearInputFields();
                    currentPage = 1; // Go to the first page to see the new entry
                    loadPage(); // Refresh the list
                } else {
                    Toast.makeText(this, "Failed to save vitals.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for all fields.", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearInputFields() {
        etTemperature.setText("");
        etHeartRate.setText("");
        etSystolic.setText("");
        etDiastolic.setText("");
    }
}
