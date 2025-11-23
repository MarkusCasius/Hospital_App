package com.example.hospimanagmenetapp.domain;

import android.content.Context;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import java.util.Calendar;
import java.util.List;

public class GetAppointmentsUseCase {
    private final AppDatabase db;

    public enum DateFilter {
        ALL, TODAY, PAST, FUTURE
    }

    public GetAppointmentsUseCase(Context context) {
        this.db = AppDatabase.getInstance(context);
    }

    public List<Appointment> execute(String clinic, DateFilter dateFilter) {
        long now = System.currentTimeMillis();
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);

        Calendar todayEnd = (Calendar) todayStart.clone();
        todayEnd.add(Calendar.DAY_OF_YEAR, 1);

        switch (dateFilter) {
            case TODAY:
                return db.appointmentDao().getAppointmentsForClinicBetween(clinic, todayStart.getTimeInMillis(), todayEnd.getTimeInMillis());
            case PAST:
                return db.appointmentDao().getAppointmentsForClinicBefore(clinic, now);
            case FUTURE:
                return db.appointmentDao().getAppointmentsForClinicAfter(clinic, now);
            case ALL:
            default:
                return db.appointmentDao().getAllAppointmentsForClinic(clinic);
        }
    }
}
