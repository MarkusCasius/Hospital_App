package com.example.hospimanagmenetapp.network;

import com.example.hospimanagmenetapp.network.dto.ClinicalRecordDto;
import com.example.hospimanagmenetapp.network.dto.VitalsDto;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface EhrApi {

    @GET("ehr/record")
    Call<List<ClinicalRecordDto>> getRecord(@Query("enPatientNhs") String patientNhs);

    @POST("ehr/updateOrCreate")
    Call<ClinicalRecordDto> updateorcreateRecord(@Body ClinicalRecordDto request);

    @POST("ehr/vitals")
    Call<Void> uploadVitals(@Body VitalsDto vitals);
}
