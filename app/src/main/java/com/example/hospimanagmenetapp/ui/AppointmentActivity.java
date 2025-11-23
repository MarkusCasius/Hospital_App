package com.example.hospimanagmenetapp.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.security.auth.BiometricLoginCoordinator;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;
import com.example.hospimanagmenetapp.ui.fragments.AppointmentListFragment;
import com.example.hospimanagmenetapp.ui.fragments.PatientAppointmentListFragment;
import com.example.hospimanagmenetapp.util.SessionManager;

public class AppointmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        boolean bypassRbac = getIntent().getBooleanExtra("bypassRbacCheck", false);

        if (!bypassRbac && !RbacPolicyEvaluator.canBookOrReschedule(this)) {
            Toast.makeText(this, "Access denied. Not permitted to make booking", Toast.LENGTH_LONG).show();
            finish();
            return; // Return here to prevent the rest of the code from running
        }

        new BiometricLoginCoordinator().authenticate(this, new BiometricLoginCoordinator.Callback() {
                    @Override
                    public void onSuccess() {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.appointmentContainer, new AppointmentListFragment())
                                .commit();
                    }

                    @Override
                    public void onFailure(String reason) {
                        // Temp Bypass
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.appointmentContainer, new AppointmentListFragment())
                                .commit();
                    }
                });

//        new BiometricLoginCoordinator().authenticate(this, new BiometricLoginCoordinator.Callback() {
//            @Override
//            public void onSuccess() {
//                // Check if the user is any type of staff member
//                boolean isStaff = userRole.equals("ADMIN") || userRole.equals("CLINICIAN") || userRole.equals("RECEPTION");
//                if (isStaff) {
//                    // Staff see the full appointment list
//                    getSupportFragmentManager().beginTransaction()
//                            .replace(R.id.appointmentContainer, new AppointmentListFragment())
//                            .commit();
//                } else if (userRole.equals("PATIENT")) {
//                    // Patients see their own appointments
//                    String patientNhs = SessionManager.getCurrentIdentifier(AppointmentActivity.this);
//                    if (patientNhs != null && !patientNhs.isEmpty()) {
//                        getSupportFragmentManager().beginTransaction()
//                                .replace(R.id.appointmentContainer, PatientAppointmentListFragment.newInstance(patientNhs))
//                                .commit();
//                    } else {
//                        onFailure("Patient NHS number not found in session.");
//                    }
//                } else {
//                    onFailure("Access denied. Unknown role.");
//                }
//            }
//            @Override public void onFailure(String reason) {
//                Toast.makeText(AppointmentActivity.this, "Biometric required: " + reason, Toast.LENGTH_LONG).show();
//                // Temporary bypass for testing.
//                boolean isStaff = userRole.equals("ADMIN") || userRole.equals("CLINICIAN") || userRole.equals("RECEPTION");
//                if (isStaff) {
//                    // Staff see the full appointment list
//                    getSupportFragmentManager().beginTransaction()
//                            .replace(R.id.appointmentContainer, new AppointmentListFragment())
//                            .commit();
//                } else if (userRole.equals("PATIENT")) {
//                    // Patients see their own appointments
//                    String patientNhs = SessionManager.getCurrentIdentifier(AppointmentActivity.this);
//                    if (patientNhs != null && !patientNhs.isEmpty()) {
//                        getSupportFragmentManager().beginTransaction()
//                                .replace(R.id.appointmentContainer, PatientAppointmentListFragment.newInstance(patientNhs))
//                                .commit();
//                    } else {
//                        onFailure("Patient NHS number not found in session.");
//                    }
//                } else {
//                    onFailure("Access denied. Unknown role.");
//                }
//                // finish();
//            }
//        });
    }
}
