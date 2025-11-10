package com.example.hospimanagmenetapp.data.repo;
import android.content.Context;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.dao.AppointmentDao;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.network.ApiClient;
import com.example.hospimanagmenetapp.network.dto.AppointmentDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class AppointmentRepository {

    private final AppointmentDao dao;
    private final ApiClient api;

    public AppointmentRepository(Context ctx) {
        this.dao = AppDatabase.getInstance(ctx).appointmentDao();
        this.api = new ApiClient(ctx);
    }

    public List<Appointment> getTodaysAppointments(String clinic, long start, long end) throws Exception {
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
            dao.upsert(a);
        }

        if (clinic != null) {
            // If a specific clinic is provided, use the filtering query
            return dao.getAppointmentsByClinicBetween(clinic, start, end);
        } else {
            // If clinic is null (i.e., "All Clinics" was selected), get all appointments
            return dao.getAllAppointmentsBetween(start, end);
        }
        // return from DB (source of truth)
        // return dao.findBetween(start, end);
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

        Response<AppointmentDto> resp = api.appointmentApi().bookOrReschedule(dto).execute();
        if (resp.isSuccessful() && resp.body() != null) {
            Appointment saved = map(resp.body());
            if (saved.id == 0) { // mock may return id=0, keep local
                saved.id = appt.id;
            }
            if (appt.id == 0) dao.upsert(saved); else dao.update(saved);
            return saved;
        } else {
            throw new IllegalStateException("Booking failed");
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
