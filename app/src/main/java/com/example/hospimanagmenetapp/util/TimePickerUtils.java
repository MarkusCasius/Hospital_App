package com.example.hospimanagmenetapp.util;

import android.app.TimePickerDialog;
import android.content.Context;
import java.util.Calendar;
import java.util.function.Consumer;

public class TimePickerUtils {

    private TimePickerUtils() {}


    public static void showTimePickerDialog(Context context, final Calendar calendar, final Runnable onTimeSetAction) {
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            onTimeSetAction.run();
        };

        new TimePickerDialog(context, timeSetListener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true)
                .show();
    }
}