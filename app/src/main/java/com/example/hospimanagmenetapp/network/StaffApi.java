package com.example.hospimanagmenetapp.network;

import com.example.hospimanagmenetapp.network.dto.StaffDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface StaffApi {

    @GET("staff")
    Call<List<StaffDto>> getAllStaff();

    @POST("staff/register")
    Call<StaffDto> registerStaff(@Body StaffDto staff);

    @DELETE("staff/{id}")
    Call<Void> deleteStaff(@Path("id") long staffId);
}