package com.example.hospimanagmenetapp.feature.appointments.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.security.auth.BiometricLoginCoordinator;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;

public class AppointmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        if (!RbacPolicyEvaluator.canViewAppointments(this)) {
            Toast.makeText(this, "Access denied. Please sign in with a permitted role.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        new BiometricLoginCoordinator().authenticate(this, new BiometricLoginCoordinator.Callback() {
            @Override public void onSuccess() {
                if (savedInstanceState == null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.appointmentContainer, new AppointmentListFragment())
                            .commit();
                }
            }
            @Override public void onFailure(String reason) {
                Toast.makeText(AppointmentActivity.this, "Biometric required: " + reason, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
