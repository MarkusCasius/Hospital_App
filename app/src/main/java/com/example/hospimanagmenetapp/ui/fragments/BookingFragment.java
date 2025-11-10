package com.example.hospimanagmenetapp.ui.fragments;


import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.domain.BookOrRescheduleAppointmentUseCase;
import com.example.hospimanagmenetapp.domain.DetectScheduleConflictsUseCase;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;

import java.util.concurrent.Executors;

public class BookingFragment extends Fragment {

    private static final String ARG_CLINICIAN_ID = "clinicianId";
    private static final String ARG_CLINICIAN_NAME = "clinicianName";
    private static final String ARG_PATIENT_NHS = "patientNhs";
    private static final String ARG_START = "start";
    private static final String ARG_END = "end";
    private static final String ARG_CLINIC = "clinic";

    public static BookingFragment newInstance(Appointment a) {
        Bundle b = new Bundle();
        b.putLong(ARG_CLINICIAN_ID, a.clinicianId);
        b.putString(ARG_CLINICIAN_NAME, a.clinicianName);
        b.putString(ARG_PATIENT_NHS, a.patientNhsNumber);
        b.putLong(ARG_START, a.startTime);
        b.putLong(ARG_END, a.endTime);
        b.putString(ARG_CLINIC, a.clinic);
        BookingFragment f = new BookingFragment();
        f.setArguments(b);
        return f;
    }

    private EditText etStart, etEnd, etNhs;
    private TextView tvClinician;
    private Button btnConfirm;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_booking, container, false);

        tvClinician = v.findViewById(R.id.tvClinician);
        etNhs = v.findViewById(R.id.etNhsBooking);
        etStart = v.findViewById(R.id.etStartMillis);
        etEnd = v.findViewById(R.id.etEndMillis);
        btnConfirm = v.findViewById(R.id.btnConfirmBooking);

        Bundle args = getArguments();
        if (args != null) {
            tvClinician.setText("Clinician: " + args.getString(ARG_CLINICIAN_NAME, ""));
            etNhs.setText(args.getString(ARG_PATIENT_NHS, ""));
            etStart.setText(String.valueOf(args.getLong(ARG_START)));
            etEnd.setText(String.valueOf(args.getLong(ARG_END)));
        }

        btnConfirm.setOnClickListener(v1 -> confirm());
        return v;
    }

    private void confirm() {
        if (!RbacPolicyEvaluator.canBookOrReschedule(requireContext())) {
            Toast.makeText(getContext(), "You do not have permission to book.", Toast.LENGTH_LONG).show();
            return;
        }

        String nhs = etNhs.getText().toString().trim();
        String startStr = etStart.getText().toString().trim();
        String endStr = etEnd.getText().toString().trim();

        if (TextUtils.isEmpty(nhs) || TextUtils.isEmpty(startStr) || TextUtils.isEmpty(endStr)) {
            Toast.makeText(getContext(), "NHS, start and end are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        long clinicianId = getArguments().getLong(ARG_CLINICIAN_ID);
        long start = Long.parseLong(startStr);
        long end = Long.parseLong(endStr);
        String clinic = getArguments().getString(ARG_CLINIC);

        // conflict detection
        boolean conflict = new DetectScheduleConflictsUseCase(requireContext()).hasConflict(clinicianId, start, end);
        if (conflict) {
            Toast.makeText(getContext(), "Time conflict detected. Choose another slot.", Toast.LENGTH_LONG).show();
            return;
        }

        Appointment a = new Appointment();
        a.patientNhsNumber = nhs;
        a.clinicianId = clinicianId;
        a.clinicianName = getArguments().getString(ARG_CLINICIAN_NAME);
        a.startTime = start;
        a.endTime = end;
        a.clinic = clinic;
        a.status = "BOOKED";

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                new BookOrRescheduleAppointmentUseCase(requireContext()).execute(a);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Appointment confirmed.", Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager().popBackStack(); // back to list
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Booking failed. Try again.", Toast.LENGTH_LONG).show());
            }
        });
    }
}