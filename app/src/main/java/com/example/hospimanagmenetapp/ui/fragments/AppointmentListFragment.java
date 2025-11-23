package com.example.hospimanagmenetapp.ui.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.entities.Clinic;
import com.example.hospimanagmenetapp.domain.GetClinicsUseCase;
import com.example.hospimanagmenetapp.domain.GetAppointmentsUseCase;
import com.example.hospimanagmenetapp.ui.adapters.AppointmentAdapter;
import com.example.hospimanagmenetapp.security.EncryptionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.Arrays;

public class AppointmentListFragment extends Fragment {

    // After all the checks with AppointmentActivity, the fragment then loads the data into the UI,
    // communicating with the database to get the appointments based on filters.


    private Spinner spClinic, spDateFilter;
    private ProgressBar progress;
    private RecyclerView rv;
    private List<Clinic> availableClinics = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_appointment_list, container, false);
        spClinic = v.findViewById(R.id.spClinic);
        spDateFilter = v.findViewById(R.id.spDateFilter);
        progress = v.findViewById(R.id.progress);
        rv = v.findViewById(R.id.rvAppointments);
        FloatingActionButton fabBookAppointment = v.findViewById(R.id.fabBookAppointment);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        setupFilters();

        v.findViewById(R.id.btnRefresh).setOnClickListener(b -> loadData());

        fabBookAppointment.setOnClickListener(view -> {
            // Creates a new, empty appointment object to pass to the BookingFragment
            Appointment newAppointment = new Appointment();

            Calendar cal = Calendar.getInstance();
            newAppointment.startTime = cal.getTimeInMillis();
            cal.add(Calendar.HOUR, 1);
            newAppointment.endTime = cal.getTimeInMillis();
            newAppointment.clinicianId = 0; // Default clinician
            newAppointment.enClinicianName = "Unassigned"; // Default name
            newAppointment.clinic = "North Clinic"; // Default clinic

            BookingFragment f = BookingFragment.newInstance(newAppointment);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.appointmentContainer, f)
                    .addToBackStack(null)
                    .commit();
        });
        return v;
    }

    private void setupFilters() {
        AdapterView.OnItemSelectedListener filterChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData(); // Call loadData whenever a selection is made.
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };

        // Clinic filter
        ArrayAdapter<String> clinicsAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spClinic.setAdapter(clinicsAdapter);
        spClinic.setOnItemSelectedListener(filterChangeListener);

        // Date filter
        ArrayAdapter<GetAppointmentsUseCase.DateFilter> dateFilters = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, GetAppointmentsUseCase.DateFilter.values());
        spDateFilter.setAdapter(dateFilters);
        spDateFilter.setSelection(Arrays.asList(GetAppointmentsUseCase.DateFilter.values()).indexOf(GetAppointmentsUseCase.DateFilter.TODAY));
        spDateFilter.setOnItemSelectedListener(filterChangeListener);

        loadClinics();
    }

    private void loadClinics() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                availableClinics = new GetClinicsUseCase(requireContext()).execute();
                List<String> clinicNames = new ArrayList<>();
                clinicNames.add("All Clinics"); // Add the "All" option first
                for (Clinic clinic : availableClinics) {
                    clinicNames.add(clinic.name);
                }

                requireActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spClinic.getAdapter();
                    adapter.clear();
                    adapter.addAll(clinicNames);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e("AppointmentList", "Failed to load clinics", e);
            }
        });
    }

    private void loadData() {
        if (spClinic.getAdapter() == null || spClinic.getSelectedItem() == null ||
                spDateFilter.getAdapter() == null || spDateFilter.getSelectedItem() == null) {
            return;
        }

        progress.setVisibility(View.VISIBLE);
        String clinic = spClinic.getSelectedItemPosition() == 0 ? null : spClinic.getSelectedItem().toString();
        GetAppointmentsUseCase.DateFilter dateFilter = (GetAppointmentsUseCase.DateFilter) spDateFilter.getSelectedItem();


        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                GetAppointmentsUseCase useCase = new GetAppointmentsUseCase(requireContext());
                List<Appointment> list = useCase.execute(clinic, dateFilter);
                List<Appointment> decryptedAppointments = EncryptionManager.decryptAppointments(list);

                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    rv.setAdapter(new AppointmentAdapter(decryptedAppointments, item -> {
                        BookingFragment f = BookingFragment.newInstance(item);
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.appointmentContainer, f)
                                .addToBackStack(null)
                                .commit();
                    }));
                });
            } catch (Exception e) {
                Log.e("AppointmentListFragment", "Failed to load appointments", e);
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load. Please retry.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
