package com.example.hospimanagmenetapp.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.security.auth.BiometricLoginCoordinator;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;
import com.example.hospimanagmenetapp.ui.fragments.AppointmentListFragment;

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

//        BiometricLoginCoordinator coordinator = new BiometricLoginCoordinator(this);
//        coordinator.authenticate(new BiometricLoginCoordinator.AuthenticationCallback() {
//            @Override
//            public void onAuthenticationSuccess() {
//                // If biometrics succeed, load the fragment.
//                if (savedInstanceState == null) {
//                    getSupportFragmentManager().beginTransaction()
//                            .replace(R.id.appointmentContainer, new AppointmentListFragment())
//                            .commit();
//                }
//            }
//
//            @Override
//            public void onAuthenticationFailed(String reason) {
//                // If biometrics fail for any reason (including user cancellation), show an error and close the activity.
//                Toast.makeText(AppointmentActivity.this, "Biometric authentication failed: " + reason, Toast.LENGTH_LONG).show();
//                finish();
//            }

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
                // Temporary bypass for testing.
                if (savedInstanceState == null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.appointmentContainer, new AppointmentListFragment())
                            .commit();
                }
                // finish();
            }
        });
    }
}
