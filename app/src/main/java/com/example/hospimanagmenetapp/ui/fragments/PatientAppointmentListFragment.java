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
import com.example.hospimanagmenetapp.data.dao.AppointmentDao;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.ui.adapters.AppointmentAdapter;

import java.util.List;
import java.util.concurrent.Executors;

public class PatientAppointmentListFragment extends Fragment {

    private static final String ARG_PATIENT_NHS = "patient_nhs";
    private RecyclerView rvPatientAppointments;
    private ProgressBar patientProgress;
    private TextView tvNoAppointments;
    private String patientNhsNumber;

    /**
     * Factory method to create a new instance of this fragment
     * using the provided patient NHS number.
     */
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
            AppDatabase db = AppDatabase.getInstance(requireContext());
            // This assumes you have an AppointmentDao with this method.
            // If not, you will need to create it.
            List<Appointment> appointments = db.appointmentDao().getAppointmentsForPatient(patientNhsNumber);

            requireActivity().runOnUiThread(() -> {
                patientProgress.setVisibility(View.GONE);
                if (appointments == null || appointments.isEmpty()) {
                    tvNoAppointments.setVisibility(View.VISIBLE);
                } else {
                    rvPatientAppointments.setVisibility(View.VISIBLE);
                    // Re-use the existing AppointmentAdapter.
                    rvPatientAppointments.setAdapter(new AppointmentAdapter(appointments, item -> {
                        // For a patient, clicking an item should open a detail view.
                        // We can re-use BookingFragment, but it should be read-only for patients.
                        // The RBAC check inside BookingFragment will prevent them from making changes.
                        BookingFragment bookingFragment = BookingFragment.newInstance(item);
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.appointmentContainer, bookingFragment)
                                .addToBackStack(null) // Allows user to navigate back to their list
                                .commit();
                    }));
                }
            });
        });
    }
}
