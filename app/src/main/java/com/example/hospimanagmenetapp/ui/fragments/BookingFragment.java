package com.example.hospimanagmenetapp.ui.fragments;


import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.entities.Staff;
import com.example.hospimanagmenetapp.domain.BookOrRescheduleAppointmentUseCase;
import com.example.hospimanagmenetapp.domain.DetectScheduleConflictsUseCase;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;
import com.example.hospimanagmenetapp.util.DatePickerUtils;
import com.example.hospimanagmenetapp.util.EncryptionManager;
import com.example.hospimanagmenetapp.util.TimePickerUtils;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class BookingFragment extends Fragment {

    private static final String ARG_CLINICIAN_ID = "clinicianId";
    private static final String ARG_CLINICIAN_NAME = "clinicianName";
    private static final String ARG_PATIENT_NHS = "patientNhs";
    private static final String ARG_START = "start";
    private static final String ARG_END = "end";
    private static final String ARG_CLINIC = "clinic";
    private static final String ARG_APPOINTMENT = "appointment";


    public static BookingFragment newInstance(Appointment a) {
        BookingFragment fragment = new BookingFragment();
        Bundle args = new Bundle();
        args.putLong("id", a.id);
        args.putString("patientNhs", a.patientNhsNumber);
        args.putLong("startTime", a.startTime);
        args.putLong("endTime", a.endTime);
        args.putLong("clinicianId", a.clinicianId);
        args.putString("clinic", a.clinic);
        fragment.setArguments(args);
        return fragment;
    }


    private EditText etStart, etEnd, etNhs;
    private Spinner spinnerClinic, spinnerClinician;
    private Button btnConfirm;

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat dateTimeFormatter;

    private List<Staff> availableClinicians = new ArrayList<>();
    private ArrayAdapter<String> clinicianAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_booking, container, false);

        spinnerClinic = v.findViewById(R.id.spinnerClinicBooking);
        spinnerClinician = v.findViewById(R.id.spinnerClinicianBooking);
        etNhs = v.findViewById(R.id.etNhsBooking);
        etStart = v.findViewById(R.id.etStartMillis);
        etEnd = v.findViewById(R.id.etEndMillis);
        btnConfirm = v.findViewById(R.id.btnConfirmBooking);

        dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.UK);

        setupSpinners();
        loadCliniciansFromDb();
        setupDateTimePickers();

        btnConfirm.setOnClickListener(v1 -> confirm());
        return v;
    }

    private void setupDateTimePickers() {
        etStart.setOnClickListener(v -> {
            // First show Date picker, then on success, show Time picker
            DatePickerUtils.showDatePickerDialog(requireContext(), startCalendar, () -> {
                TimePickerUtils.showTimePickerDialog(requireContext(), startCalendar, () -> {
                    etStart.setText(dateTimeFormatter.format(startCalendar.getTime()));
                });
            });
        });

        etEnd.setOnClickListener(v -> {
            DatePickerUtils.showDatePickerDialog(requireContext(), endCalendar, () -> {
                TimePickerUtils.showTimePickerDialog(requireContext(), endCalendar, () -> {
                    etEnd.setText(dateTimeFormatter.format(endCalendar.getTime()));
                });
            });
        });
    }

    private void setupSpinners() {
        // Setup Clinic Spinner
        ArrayAdapter<String> clinicAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"North Clinic", "South Clinic"});
        spinnerClinic.setAdapter(clinicAdapter);

        // Setup Clinician Spinner
        clinicianAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinnerClinician.setAdapter(clinicianAdapter);
    }

    private void loadCliniciansFromDb() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<Staff> encryptedClinicians = db.staffDao().getClinicians();
                EncryptionManager encryptionManager = new EncryptionManager();
                List<Staff> decryptedClinicians = new ArrayList<>();
                List<String> clinicianNames = new ArrayList<>();

                for (Staff encryptedStaff : encryptedClinicians) {
                    try {
                        String decryptedName = encryptionManager.decrypt(encryptedStaff.fullName);
                        Staff decryptedStaff = new Staff();
                        decryptedStaff.id = encryptedStaff.id;
                        decryptedStaff.fullName = decryptedName;
                        decryptedStaff.email = encryptionManager.decrypt(encryptedStaff.email);
                        decryptedStaff.role = encryptedStaff.role;

                        decryptedClinicians.add(decryptedStaff);
                        clinicianNames.add(decryptedName);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to decrypt clinician: " + encryptedStaff.id, e);
                    }
                }
                availableClinicians = decryptedClinicians;

                requireActivity().runOnUiThread(() -> {
                    clinicianAdapter.clear();
                    clinicianAdapter.addAll(clinicianNames);
                    clinicianAdapter.notifyDataSetChanged();
                    populateInitialData(); // Now populate fields after data is loaded
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to load clinicians from DB", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error loading clinicians.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void populateInitialData() {
        Bundle args = getArguments();
        if (args == null) return;

        etNhs.setText(args.getString("patientNhs", ""));
        long startTimeMillis = args.getLong("startTime");
        if (startTimeMillis > 0) {
            startCalendar.setTimeInMillis(startTimeMillis);
            etStart.setText(dateTimeFormatter.format(startCalendar.getTime()));
        }

        long endTimeMillis = args.getLong("endTime");
        if (endTimeMillis > 0) {
            endCalendar.setTimeInMillis(endTimeMillis);
            etEnd.setText(dateTimeFormatter.format(endCalendar.getTime()));
        }

        // Set clinic spinner selection
        String initialClinic = args.getString("clinic");
        if (initialClinic != null) {
            for (int i = 0; i < spinnerClinic.getAdapter().getCount(); i++) {
                if (initialClinic.equals(spinnerClinic.getItemAtPosition(i).toString())) {
                    spinnerClinic.setSelection(i);
                    break;
                }
            }
        }

        // Set clinician spinner selection
        long initialClinicianId = args.getLong("clinicianId");
        for (int i = 0; i < availableClinicians.size(); i++) {
            if (availableClinicians.get(i).id == initialClinicianId) {
                spinnerClinician.setSelection(i);
                break;
            }
        }
    }


    private void confirm() {
        if (!RbacPolicyEvaluator.canBookOrReschedule(requireContext())) {
            Toast.makeText(getContext(), "You do not have permission to book.", Toast.LENGTH_LONG).show();
            return;
        }

        String nhs = etNhs.getText().toString().trim();
        long start = startCalendar.getTimeInMillis();
        long end = endCalendar.getTimeInMillis();


        if (spinnerClinician.getSelectedItem() == null || TextUtils.isEmpty(nhs) || start <= 0 || end <= 0) {
            Toast.makeText(getContext(), "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (end <= start) {
            Toast.makeText(getContext(), "End time must be after start time.", Toast.LENGTH_SHORT).show();
            return;
        }


        int selectedClinicianPosition = spinnerClinician.getSelectedItemPosition();
        Staff selectedClinician = availableClinicians.get(selectedClinicianPosition);
        long clinicianId = selectedClinician.id;
        String clinicianName = selectedClinician.fullName;

        // Get selected clinic
        String clinic = spinnerClinic.getSelectedItem().toString();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Conflict detection
                boolean conflict = new DetectScheduleConflictsUseCase(requireContext()).hasConflict(clinicianId, start, end);
                if (conflict) {
                    Toast.makeText(getContext(), "Time conflict detected. Choose another slot.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Create an Appointment object and crucially set its ID from the arguments.
                Appointment appointmentToSave = new Appointment();
                // getArguments() will contain the ID. If new, it defaults to 0.
                appointmentToSave.id = getArguments().getLong("id", 0);

                // Populate the rest of the details from the UI
                appointmentToSave.patientNhsNumber = nhs;
                appointmentToSave.clinicianId = clinicianId;
                appointmentToSave.clinicianName = clinicianName;
                appointmentToSave.startTime = start;
                appointmentToSave.endTime = end;
                appointmentToSave.clinic = clinic;
                appointmentToSave.status = "BOOKED";

                new BookOrRescheduleAppointmentUseCase(requireContext()).execute(appointmentToSave);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Appointment confirmed.", Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager().popBackStack(); // Go back to list
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to save appointment", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Booking failed. Please try again.", Toast.LENGTH_LONG).show());
            }
        });
    }
}