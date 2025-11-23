package com.example.hospimanagmenetapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.ui.adapters.AppointmentAdapter;
import com.example.hospimanagmenetapp.security.EncryptionManager;

import java.util.List;
import java.util.concurrent.Executors;

public class PatientAppointmentListFragment extends Fragment {

    // A seperate fragment for displaying appointments to patients. However, currently is neglected
    // due to having to focus on other features.

    private static final String ARG_PATIENT_NHS = "patient_nhs";
    private RecyclerView rvPatientAppointments;
    private ProgressBar patientProgress;
    private TextView tvNoAppointments;
    private String patientNhsNumber;

    public static PatientAppointmentListFragment newInstance(String patientNhs) {
        PatientAppointmentListFragment fragment = new PatientAppointmentListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PATIENT_NHS, patientNhs);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientNhsNumber = getArguments().getString(ARG_PATIENT_NHS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_appointment_list, container, false);
        rvPatientAppointments = view.findViewById(R.id.rvPatientAppointments);
        patientProgress = view.findViewById(R.id.patientProgress);
        tvNoAppointments = view.findViewById(R.id.tvNoAppointments);

        rvPatientAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        loadAppointments();

        return view;
    }

    private void loadAppointments() {
        if (patientNhsNumber == null || patientNhsNumber.isEmpty()) {
            Toast.makeText(getContext(), "Patient identifier missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        patientProgress.setVisibility(View.VISIBLE);
        rvPatientAppointments.setVisibility(View.GONE);
        tvNoAppointments.setVisibility(View.GONE);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<Appointment> appointments = db.appointmentDao().getAppointmentsForPatient(patientNhsNumber);

                List<Appointment> decryptedList = EncryptionManager.decryptAppointments(appointments);

                requireActivity().runOnUiThread(() -> {
                    patientProgress.setVisibility(View.GONE);
                    if (decryptedList == null || decryptedList.isEmpty()) {
                        tvNoAppointments.setVisibility(View.VISIBLE);
                    } else {
                        rvPatientAppointments.setVisibility(View.VISIBLE);
                        rvPatientAppointments.setAdapter(new AppointmentAdapter(decryptedList, item -> {
                            BookingFragment bookingFragment = BookingFragment.newInstance(item);
                            requireActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.appointmentContainer, bookingFragment)
                                    .addToBackStack(null) // Allows user to navigate back to their list
                                    .commit();
                        }));
                    }
                });
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error loading appointments.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
