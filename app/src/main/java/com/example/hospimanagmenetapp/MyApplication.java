package com.example.hospimanagmenetapp;

import android.app.Application;

import com.example.hospimanagmenetapp.util.AppointmentSeeder;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // This is the perfect place to call your seeder.
        // It runs once when the application process is created,
        // ensuring data is ready before any activity is shown.
        AppointmentSeeder.seed(this);
    }
}
