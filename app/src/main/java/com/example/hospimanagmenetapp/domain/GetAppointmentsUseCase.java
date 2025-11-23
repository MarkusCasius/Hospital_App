package com.example.hospimanagmenetapp.domain;

import android.content.Context;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.repo.AppointmentRepository;
import java.util.Calendar;
import java.util.List;

public class GetAppointmentsUseCase {
    private final AppointmentRepository repo;

    public enum DateFilter {
        ALL, TODAY, PAST, FUTURE
    }

    public GetAppointmentsUseCase(Context context) {
        this.repo = new AppointmentRepository(context);
    }

    // Domain for getting appointments, based on a filter and a clinic
    public List<Appointment> execute(String clinic, DateFilter dateFilter) throws Exception {
        long now = System.currentTimeMillis();
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);

        Calendar todayEnd = (Calendar) todayStart.clone();
        todayEnd.add(Calendar.DAY_OF_YEAR, 1);

        switch (dateFilter) {
            case TODAY:
                return repo.getAppointmentsBetween(clinic, todayStart.getTimeInMillis(), todayEnd.getTimeInMillis());
            case PAST:
                return repo.getAppointmentsBefore(clinic, now);
            case FUTURE:
                return repo.getAppointmentsAfter(clinic, now);
            case ALL:
            default:
                return repo.getAllAppointmentsForClinic(clinic);
        }
    }
}
