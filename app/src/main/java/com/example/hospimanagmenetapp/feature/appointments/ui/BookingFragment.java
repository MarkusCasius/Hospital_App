package com.example.hospimanagmenetapp.feature.appointments.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
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

    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());

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
            startCalendar.setTimeInMillis(args.getLong(ARG_START));
            endCalendar.setTimeInMillis(args.getLong(ARG_END));
            etStart.setText(dateTimeFormatter.format(startCalendar.getTime()));
            etEnd.setText(dateTimeFormatter.format(endCalendar.getTime()));
        }

        etStart.setOnClickListener(view -> showDateTimePicker(startCalendar, etStart));
        etEnd.setOnClickListener(view -> showDateTimePicker(endCalendar, etEnd));

        btnConfirm.setOnClickListener(v1 -> confirm());
        return v;
    }

    private void showDateTimePicker(final Calendar calendar, final EditText editText) {
        // First, show DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // After setting the date, show TimePickerDialog
                    new TimePickerDialog(
                            requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                // Update the EditText with the formatted date and time
                                editText.setText(dateTimeFormatter.format(calendar.getTime()));
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false // Use false for 12-hour format with AM/PM
                    ).show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void confirm() {
        if (!RbacPolicyEvaluator.canBookOrReschedule(requireContext())) {
            Toast.makeText(getContext(), "You do not have permission to book.", Toast.LENGTH_LONG).show();
            return;
        }

        String nhs = etNhs.getText().toString().trim();

        if (TextUtils.isEmpty(nhs)) {
            Toast.makeText(getContext(), "NHS number is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        long clinicianId = getArguments().getLong(ARG_CLINICIAN_ID);
        long start = startCalendar.getTimeInMillis();
        long end = endCalendar.getTimeInMillis();
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
