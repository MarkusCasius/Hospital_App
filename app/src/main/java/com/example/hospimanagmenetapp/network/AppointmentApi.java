package com.example.hospimanagmenetapp.network;

import com.example.hospimanagmenetapp.network.dto.AppointmentDto;
import com.example.hospimanagmenetapp.network.dto.ClinicDto;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AppointmentApi {

    @GET("appointments")
    Call<List<AppointmentDto>> getAppointments();

    @GET("clinics")
    Call<List<ClinicDto>> getClinics();

    @POST("appointments/bookOrReschedule")
    Call<AppointmentDto> bookOrReschedule(@Body AppointmentDto request);
}

