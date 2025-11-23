package com.example.hospimanagmenetapp.domain;

import android.content.Context;

import com.example.hospimanagmenetapp.data.entities.Clinic;
import com.example.hospimanagmenetapp.data.repo.AppointmentRepository;

import java.util.List;

public class GetClinicsUseCase {
    private final AppointmentRepository repo;

    public GetClinicsUseCase(Context context) {
        this.repo = new AppointmentRepository(context);
    }

    // Domain for getting clinics to then be used in later methods/functions
    public List<Clinic> execute() {
        return repo.getAndCacheClinics();
    }
}
