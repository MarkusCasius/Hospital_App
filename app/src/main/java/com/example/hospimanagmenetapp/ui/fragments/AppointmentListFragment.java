package com.example.hospimanagmenetapp.ui.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.domain.GetTodaysAppointmentsUseCase;
import com.example.hospimanagmenetapp.ui.adapters.AppointmentAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class AppointmentListFragment extends Fragment {

    private Spinner spClinic;
    private ProgressBar progress;
    private androidx.recyclerview.widget.RecyclerView rv;
    private FloatingActionButton fabBookAppointment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_appointment_list, container, false);
        spClinic = v.findViewById(R.id.spClinic);
        progress = v.findViewById(R.id.progress);
        rv = v.findViewById(R.id.rvAppointments);
        fabBookAppointment = v.findViewById(R.id.fabBookAppointment);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayAdapter<String> clinics = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All Clinics", "North Clinic", "South Clinic"});
        spClinic.setAdapter(clinics);

        v.findViewById(R.id.btnRefresh).setOnClickListener(b -> loadData());

        fabBookAppointment.setOnClickListener(view -> {
            // Creates a new, empty appointment object to pass to the BookingFragment
            Appointment newAppointment = new Appointment();

            Calendar cal = Calendar.getInstance();
            newAppointment.startTime = cal.getTimeInMillis();
            cal.add(Calendar.HOUR, 1);
            newAppointment.endTime = cal.getTimeInMillis();
            newAppointment.clinicianId = 0; // Or a default ID
            newAppointment.clinicianName = "Unassigned"; // Default name
            newAppointment.clinic = "North Clinic"; // Default clinic

            BookingFragment f = BookingFragment.newInstance(newAppointment);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.appointmentContainer, f)
                    .addToBackStack(null)
                    .commit();
        });

        loadData();
        return v;
    }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);
        String clinic = spClinic.getSelectedItemPosition() == 0 ? null : spClinic.getSelectedItem().toString();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Fetchs appointments directly from the DAO.
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<Appointment> list;
                if (clinic == null) {
                    // If "All Clinics" is selected, get all appointments.
                    list = db.appointmentDao().getAllAppointments();
                } else {
                    // Otherwise, get appointments filtered by the selected clinic.
                    list = db.appointmentDao().getAppointmentsByClinic(clinic);
                }

                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    rv.setAdapter(new AppointmentAdapter(list, item -> {
                        BookingFragment f = BookingFragment.newInstance(item);
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.appointmentContainer, f)
                                .addToBackStack(null)
                                .commit();
                    }));
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load. Please retry.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}