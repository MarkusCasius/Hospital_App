package com.example.hospimanagmenetapp.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DatePickerUtils {

    // Standard date format for the app
    public static final String APP_DATE_FORMAT = "dd-MM-yyyy";

    private DatePickerUtils() {}

    public static void showDatePickerDialog(Context context, final Calendar calendar, final Runnable onDateSetAction) {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            onDateSetAction.run(); // Execute the callback
        };

        new DatePickerDialog(context, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }
}