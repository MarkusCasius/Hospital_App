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

    public static void showDatePickerDialog(Context context, final EditText targetEditText, final Calendar initialDate) {
        // Create a listener that will be called when the user sets a date.
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            // Update the calendar instance with the date chosen by the user.
            initialDate.set(Calendar.YEAR, year);
            initialDate.set(Calendar.MONTH, month);
            initialDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            // Create a formatter and set the EditText's text.
            SimpleDateFormat sdf = new SimpleDateFormat(APP_DATE_FORMAT, Locale.UK);
            targetEditText.setText(sdf.format(initialDate.getTime()));
        };

        // Create and show the DatePickerDialog.
        // It will be pre-filled with the date from the initialDate Calendar object.
        new DatePickerDialog(context, dateSetListener,
                initialDate.get(Calendar.YEAR),
                initialDate.get(Calendar.MONTH),
                initialDate.get(Calendar.DAY_OF_MONTH))
                .show();
    }
}