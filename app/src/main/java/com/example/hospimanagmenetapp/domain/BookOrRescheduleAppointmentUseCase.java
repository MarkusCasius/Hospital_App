package com.example.hospimanagmenetapp.domain;

import android.content.Context;

import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.repo.AppointmentRepository;

public class BookOrRescheduleAppointmentUseCase {
    private final AppointmentRepository repo;

    public BookOrRescheduleAppointmentUseCase(Context ctx) {
        this.repo = new AppointmentRepository(ctx);
    }

    public Appointment execute(Appointment appt) throws Exception {
        return repo.bookOrReschedule(appt);
    }
}
