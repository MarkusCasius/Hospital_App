package com.example.hospimanagmenetapp.network;

import com.example.hospimanagmenetapp.network.dto.AppointmentDto;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AppointmentApi {

    @GET("appointments/today")
    Call<List<AppointmentDto>> getTodaysAppointments(@Query("clinic") String clinic);

    @POST("appointments/bookOrReschedule")
    Call<AppointmentDto> bookOrReschedule(@Body AppointmentDto request);
}

