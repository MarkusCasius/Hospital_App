package com.example.hospimanagmenetapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Appointment;


public class AppointmentSeeder {

    private static final String PREFS_NAME = "SeederPrefs";
    private static final String KEY_SEEDED = "appointmentsSeeded";

    public static void seed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean alreadySeeded = prefs.getBoolean(KEY_SEEDED, false);

        if (alreadySeeded) return;

        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            long now = System.currentTimeMillis();

            for (int i = 0; i < 10; i++) {
                Appointment a = new Appointment();
                a.patientNhsNumber = "999999999" + i;
                a.startTime = now + i * 60 * 60 * 1000;
                a.endTime = a.startTime + 45 * 60 * 1000;
                a.clinicianId = 1000 + (i % 3);
                a.clinicianName = "Dr. Test " + (char) ('A' + i);
                a.clinic = (i % 2 == 0) ? "Surgery A" : "Surgery B";
                a.status = (i % 4 == 0) ? "CANCELLED" : "BOOKED";

                db.appointmentDao().insert(a);
            }

            prefs.edit().putBoolean(KEY_SEEDED, true).apply();
        });
    }
}
