package com.example.hospimanagmenetapp.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.feature.appointments.ui.adapters.AppointmentAdapter;

import java.util.List;
import java.util.concurrent.Executors;

public class AppointmentSchedulingActivity extends AppCompatActivity {

    private RecyclerView rvAppointment;

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Called when the Activity is created
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_scheduling); // Inflate the activity scheduling layout

        rvAppointment = findViewById(R.id.rvAppointment);

        rvAppointment.setLayoutManager(new LinearLayoutManager(this));

    }

    private void loadAppointment() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Appointment> list = AppDatabase.getInstance(getApplicationContext()).staffDao().getAll(); // Read from Room
            runOnUiThread(() -> rvAppointment.setAdapter(new AppointmentAdapter(list)));
        });
    }
}
