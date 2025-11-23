package com.example.hospimanagmenetapp.ui.fragments;


import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.entities.Clinic;
import com.example.hospimanagmenetapp.data.entities.Staff;
import com.example.hospimanagmenetapp.data.repo.StaffRepository;
import com.example.hospimanagmenetapp.domain.BookOrRescheduleAppointmentUseCase;
import com.example.hospimanagmenetapp.domain.DetectScheduleConflictsUseCase;
import com.example.hospimanagmenetapp.domain.GetClinicsUseCase;
import com.example.hospimanagmenetapp.domain.ValidatePatientExistsUseCase;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;
import com.example.hospimanagmenetapp.util.DatePickerUtils;
import com.example.hospimanagmenetapp.security.EncryptionManager;
import com.example.hospimanagmenetapp.security.RateLimiter;
import com.example.hospimanagmenetapp.util.TimePickerUtils;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BookingFragment extends Fragment {

    // Allows users to book and edit appointments, with levels of validation ensuring that there
    // isn't any conflicts.

    public static BookingFragment newInstance(Appointment a) {
        BookingFragment fragment = new BookingFragment();
        Bundle args = new Bundle();
        args.putLong("id", a.id);
        args.putString("patientNhs", a.enPatientNhsNumber);
        args.putLong("startTime", a.startTime);
        args.putLong("endTime", a.endTime);
        args.putLong("clinicianId", a.clinicianId);
        args.putString("clinic", a.clinic);
        fragment.setArguments(args);
        return fragment;
    }


    private EditText etStart, etEnd, etNhs;
    private Spinner spinnerClinic, spinnerClinician, spinnerExpertiseFilter, spinnerStatus;

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat dateTimeFormatter;

    private List<Staff> availableClinicians = new ArrayList<>();
    private List<Staff> allClinicians = new ArrayList<>();
    private List<Clinic> availableClinicsList = new ArrayList<>(); // To store clinic objects
    private ArrayAdapter<String> clinicianAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (!RbacPolicyEvaluator.canBookOrReschedule(requireContext())) {
            Toast.makeText(getContext(), "Access denied. Not permitted to make booking", Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return null;
        }

        View v = inflater.inflate(R.layout.fragment_booking, container, false);

        spinnerClinic = v.findViewById(R.id.spinnerClinicBooking);
        spinnerClinician = v.findViewById(R.id.spinnerClinicianBooking);
        spinnerExpertiseFilter = v.findViewById(R.id.spinnerExpertiseFilter);
        spinnerStatus = v.findViewById(R.id.spinnerStatus);
        etNhs = v.findViewById(R.id.etNhsBooking);
        etStart = v.findViewById(R.id.etStartMillis);
        etEnd = v.findViewById(R.id.etEndMillis);
        Button btnConfirm = v.findViewById(R.id.btnConfirmBooking);

        dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.UK);

        setupSpinners();
        loadAllCliniciansFromDb();
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
                android.R.layout.simple_spinner_dropdown_item);
        spinnerClinic.setAdapter(clinicAdapter);
        loadClinicsForBooking();

        // Setup Clinician Spinner
        clinicianAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinnerClinician.setAdapter(clinicianAdapter);

        // Setup Expertise Filter Spinner
        List<String> expertiseFilterOptions = new ArrayList<>();
        expertiseFilterOptions.add("All Expertise");
        for (Staff.Expertise expertise : Staff.Expertise.values()) {
            expertiseFilterOptions.add(expertise.name());
        }
        ArrayAdapter<String> expertiseAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, expertiseFilterOptions);
        spinnerExpertiseFilter.setAdapter(expertiseAdapter);

        // Listener to update clinician list when expertise is selected
        spinnerExpertiseFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateClinicianSpinner();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"BOOKED", "CANCELLED", "COMPLETED"});
        spinnerStatus.setAdapter(statusAdapter);
    }

    private void loadClinicsForBooking() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                availableClinicsList = new GetClinicsUseCase(requireContext()).execute();
                List<String> clinicNames = availableClinicsList.stream()
                        .map(c -> c.name)
                        .collect(Collectors.toList());

                requireActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerClinic.getAdapter();
                    adapter.clear();
                    adapter.addAll(clinicNames);
                    adapter.notifyDataSetChanged();
                    // After loading clinics, populate other data
                    populateInitialData();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load clinics for booking", e);
            }
        });
    }

    private void loadAllCliniciansFromDb() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                StaffRepository staffRepo = new StaffRepository(requireContext());
                List<Staff> encryptedClinicians = staffRepo.getAndCacheClinicians();

                // Decrypt all clinicians and store them in a master list
                allClinicians.clear();
                for (Staff encryptedStaff : encryptedClinicians) {
                    allClinicians.add(EncryptionManager.decryptStaff(encryptedStaff));
                }

                requireActivity().runOnUiThread(() -> {
                    updateClinicianSpinner(); // Update the spinner with the initial (unfiltered) list
                });


            } catch (Exception e) {
                Log.e(TAG, "Failed to load clinicians from DB", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error loading clinicians.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateClinicianSpinner() {
        String selectedExpertiseStr = (String) spinnerExpertiseFilter.getSelectedItem();
        List<Staff> filteredClinicians;

        // If "All Expertise" is selected, use the master list. Otherwise, filter it.
        if ("All Expertise".equals(selectedExpertiseStr)) {
            filteredClinicians = new ArrayList<>(allClinicians);
        } else {
            Staff.Expertise selectedExpertise = Staff.Expertise.valueOf(selectedExpertiseStr);
            filteredClinicians = allClinicians.stream()
                    .filter(c -> c.expertise == selectedExpertise)
                    .collect(Collectors.toList());
        }

        // Update the adapter data for the clinician spinner
        availableClinicians = filteredClinicians; // Update the list used by `confirm()`
        List<String> clinicianNames = filteredClinicians.stream()
                .map(s -> s.fullName)
                .collect(Collectors.toList());

        clinicianAdapter.clear();
        clinicianAdapter.addAll(clinicianNames);
        clinicianAdapter.notifyDataSetChanged();
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
        String clinic = spinnerClinic.getSelectedItem().toString();
        String status = spinnerStatus.getSelectedItem().toString();

        if (start < System.currentTimeMillis() && !status.equals("BOOKED")) {
            Toast.makeText(getContext(), "Cannot book an appointment in the past.", Toast.LENGTH_LONG).show();
            return;
        }

        RateLimiter rateLimiter = new RateLimiter(requireContext());
        if (!rateLimiter.isAttemptAllowed()) {
            Toast.makeText(getContext(), "You have made too many bookings recently. Please try again later.", Toast.LENGTH_LONG).show();
            return; // Stop the booking process
        }


        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ValidatePatientExistsUseCase validationUseCase = new ValidatePatientExistsUseCase(requireContext());
                boolean patientExists = validationUseCase.execute(nhs);

                long currentAppointmentId = getArguments().getLong("id", 0);

                // Conflict detection
                boolean conflict = new DetectScheduleConflictsUseCase(requireContext()).hasConflict(clinicianId, start, end, currentAppointmentId);
                if (conflict) {
                    requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Time conflict detected. Choose another slot.", Toast.LENGTH_LONG).show());
                    return;
                }

                if (!patientExists) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error: No patient found with the provided NHS number.", Toast.LENGTH_LONG).show());
                    return;
                }

                Appointment appointmentToSave = new Appointment();
                appointmentToSave.id = currentAppointmentId;
                appointmentToSave.id = getArguments().getLong("id", 0);
                appointmentToSave.enPatientNhsNumber = nhs; // Still plaintext here
                appointmentToSave.startTime = start;
                appointmentToSave.endTime = end;
                appointmentToSave.clinicianId = clinicianId;
                appointmentToSave.enClinicianName = clinicianName; // Still plaintext here
                appointmentToSave.clinic = clinic;
                appointmentToSave.status = status;

                Appointment encryptedAppointment = EncryptionManager.encryptAppointment(appointmentToSave);

                new BookOrRescheduleAppointmentUseCase(requireContext()).execute(encryptedAppointment);

                rateLimiter.recordNewAttempt();

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