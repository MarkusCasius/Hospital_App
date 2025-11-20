package com.example.hospimanagmenetapp.data.repo;

import android.content.Context;
import android.util.Log;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.dao.AppointmentDao;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.network.ApiClient;
import com.example.hospimanagmenetapp.network.dto.AppointmentDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Response;

public class AppointmentRepository {

    private static final String TAG = "AppointmentRepository";

    private final AppointmentDao dao;
    private final ApiClient api;

    public AppointmentRepository(Context ctx) {
        this.dao = AppDatabase.getInstance(ctx).appointmentDao();
        this.api = new ApiClient(ctx);
    }

    public List<Appointment> getTodaysAppointments(String clinic, long start, long end) throws Exception {
        try {
            // fetch mock network first
            Response<List<AppointmentDto>> resp = api.appointmentApi().getTodaysAppointments(clinic).execute();
            List<Appointment> mapped = new ArrayList<>();
            if (resp.isSuccessful() && resp.body() != null) {
                for (AppointmentDto dto : resp.body()) {
                    Appointment a = map(dto);
                    mapped.add(a);
                }
            }
            // cache to DB (simplified: insert if none today)
            for (Appointment a : mapped) {
                dao.insert(a);
            }
        } catch (Exception e) {}
        Log.d(TAG, "Fetching today's appointments directly from the database.");
        // List<Appointment> appointments = dao.findBetween(start, end);
        List<Appointment> appointments = dao.getAllAppointments();
        Log.d(TAG, "Appointments: " + appointments);
        if (clinic == null) {
            Log.d(TAG, "No clinic specified. Returning all appointments.");
            return appointments;
        }
        return appointments.stream()
                .filter(a -> clinic.equals(a.clinic))
                .collect(Collectors.toList());
    }

    public Appointment bookOrReschedule(Appointment appt) throws Exception {
        AppointmentDto dto = new AppointmentDto();
        dto.id = appt.id;
        dto.patientNhsNumber = appt.patientNhsNumber;
        dto.startTime = appt.startTime;
        dto.endTime = appt.endTime;
        dto.clinicianId = appt.clinicianId;
        dto.clinicianName = appt.clinicianName;
        dto.clinic = appt.clinic;
        dto.status = "BOOKED";

        Log.d(TAG, "Attempting to book/reschedule appointment via API. ID: " + dto.id + ", NHS: " + dto.patientNhsNumber);

        Response<AppointmentDto> resp = api.appointmentApi().bookOrReschedule(dto).execute();
        if (resp.isSuccessful() && resp.body() != null) {
            Log.i(TAG, "API call successful. Response code: " + resp.code());
            Appointment saved = map(resp.body());
            if (saved.id == 0) { // mock may return id=0, keep local
                saved.id = appt.id;
            }

            Log.d(TAG, "Saving to local DB. Original ID: " + appt.id + ", Saved ID: " + saved.id);
            if (appt.id == 0) {
                dao.insert(saved);
            } else {
                dao.update(saved);
            }
            return saved;
        } else {
            // Debugging
            String errorBody = resp.errorBody() != null ? resp.errorBody().string() : "null";
            Log.e(TAG, "Booking failed. Response was not successful.");
            Log.e(TAG, "Response Code: " + resp.code());
            Log.e(TAG, "Response Message: " + resp.message());
            Log.e(TAG, "Error Body: " + errorBody);

            throw new IllegalStateException("Booking failed: API returned code " + resp.code() + ". Check logs for details.");
        }
    }

    public List<Appointment> detectConflicts(long clinicianId, long start, long end) {
        return dao.overlapping(clinicianId, start, end);
    }

    private Appointment map(AppointmentDto dto) {
        Appointment a = new Appointment();
        a.id = dto.id;
        a.patientNhsNumber = dto.patientNhsNumber;
        a.startTime = dto.startTime;
        a.endTime = dto.endTime;
        a.clinicianId = dto.clinicianId;
        a.clinicianName = dto.clinicianName;
        a.clinic = dto.clinic;
        a.status = dto.status;
        return a;
    }
}
