package com.example.hospimanagmenetapp.data.repo;

import android.content.Context;
import android.util.Log;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.dao.AppointmentDao;
import com.example.hospimanagmenetapp.data.dao.ClinicDao;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.entities.Clinic;
import com.example.hospimanagmenetapp.network.ApiClient;
import com.example.hospimanagmenetapp.network.dto.AppointmentDto;
import com.example.hospimanagmenetapp.network.dto.ClinicDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

// A class for handling all the database interactions via calling the API. Designed with offline-first
// principles

public class AppointmentRepository {

    private static final String TAG = "AppointmentRepository";

    private final AppointmentDao dao;
    private final ApiClient api;
    private final ClinicDao clinicDao;

    public AppointmentRepository(Context ctx) {
        AppDatabase db = AppDatabase.getInstance(ctx);
        this.dao = db.appointmentDao();
        this.clinicDao = db.clinicDao();
        this.api = new ApiClient(ctx);
    }

    // Fetches and syncs with the local database any clinics in the external database
    public List<Clinic> getAndCacheClinics() {
        try {
            Log.d(TAG, "Fetching clinics from network to refresh cache.");
            Response<List<ClinicDto>> response = api.appointmentApi().getClinics().execute();

            if (response.isSuccessful() && response.body() != null) {
                List<Clinic> clinicsToCache = new ArrayList<>();
                for (ClinicDto dto : response.body()) {
                    Clinic clinic = new Clinic();
                    clinic.name = dto.name;
                    clinic.location = dto.location;
                    clinicsToCache.add(clinic);
                }
                // Cache the fresh data to the database
                clinicDao.insertAll(clinicsToCache);
                Log.d(TAG, "Successfully cached " + clinicsToCache.size() + " clinics from network.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to sync clinics from network, will use local data.", e);
        }

        // Always return the data from the local database (single source of truth)
        return clinicDao.getAll();
    }

    // Fetches and syncs with the local database any appointments in the external database
    private void refreshCachedAppointments() {
        try {
            // fetch mock network first
            Log.d(TAG, "Fetching appointments from network to cache.");
            Response<List<AppointmentDto>> resp = api.appointmentApi().getAppointments().execute();

            if (resp.isSuccessful() && resp.body() != null) {
            List<Appointment> mapped = new ArrayList<>();
                for (AppointmentDto dto : resp.body()) {
                    Appointment a = map(dto);
                    mapped.add(a);
                }
                // cache to DB (simplified: insert if none today)
                for (Appointment a : mapped) {
                    dao.insert(a);
                }
                Log.d(TAG, "Successfully fetched and cached " + mapped.size() + " appointments from network.");
            }

        } catch (Exception e) {
            Log.w(TAG, "Network call failed, will fall back to existing local data.", e);
        }
    }

    public List<Appointment> getAppointmentsBetween(String clinic, long startTime, long endTime) {
        refreshCachedAppointments();
        return dao.getAppointmentsForClinicBetween(clinic, startTime, endTime);
    }

    public List<Appointment> getAppointmentsBefore(String clinic, long endTime) {
        refreshCachedAppointments();
        return dao.getAppointmentsForClinicBefore(clinic, endTime);
    }

    public List<Appointment> getAppointmentsAfter(String clinic, long startTime) {
        refreshCachedAppointments();
        return dao.getAppointmentsForClinicAfter(clinic, startTime);
    }

    public List<Appointment> getAllAppointmentsForClinic(String clinic) {
        refreshCachedAppointments();
        return dao.getAllAppointmentsForClinic(clinic);
    }

    // Booking or rescheduling appointments. After getting the mapping it to the dto, it sends a call to the API
    // Then saves locally as the database isn't set up
    public Appointment bookOrReschedule(Appointment appt) throws Exception {
        AppointmentDto dto = new AppointmentDto();
        dto.id = appt.id;
        dto.patientNhsNumber = appt.enPatientNhsNumber;
        dto.startTime = appt.startTime;
        dto.endTime = appt.endTime;
        dto.clinicianId = appt.clinicianId;
        dto.clinicianName = appt.enClinicianName;
        dto.clinic = appt.clinic;
        dto.status = "BOOKED";

        Log.d(TAG, "Attempting to book/reschedule appointment via API. ID: ");

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
            Log.e(TAG, "Booking failed. Response was not successful.");
            Log.e(TAG, "Response Code: " + resp.code());
            Log.e(TAG, "Response Message: " + resp.message());

            throw new IllegalStateException("Booking failed: API returned code " + resp.code() + ". Check logs for details.");
        }
    }

    // Detects if there is any overlapping appointments
    public List<Appointment> detectConflicts(long clinicianId, long start, long end, long appointmentId) {
        return dao.getConflictingAppointments(clinicianId, start, end, appointmentId);
    }

    private Appointment map(AppointmentDto dto) {
        Appointment a = new Appointment();
        a.id = dto.id;
        a.enPatientNhsNumber = dto.patientNhsNumber;
        a.startTime = dto.startTime;
        a.endTime = dto.endTime;
        a.clinicianId = dto.clinicianId;
        a.enClinicianName = dto.clinicianName;
        a.clinic = dto.clinic;
        a.status = dto.status;
        return a;
    }
}
