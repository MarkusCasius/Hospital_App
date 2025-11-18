package com.example.hospimanagmenetapp;

import android.app.Application;


import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.hospimanagmenetapp.feature.ehr.work.VitalsSyncWorker;
import com.example.hospimanagmenetapp.util.DatabaseSeeder;

import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        DatabaseSeeder.seed(this);

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                VitalsSyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        // For making period checks for syncing vitals.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "vitals-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest);
    }
}
