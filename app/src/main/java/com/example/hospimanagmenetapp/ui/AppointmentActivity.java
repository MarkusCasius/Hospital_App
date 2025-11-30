package com.example.hospimanagmenetapp.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.security.RuntimeGuard;
import com.example.hospimanagmenetapp.security.auth.BiometricLoginCoordinator;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;
import com.example.hospimanagmenetapp.ui.fragments.AppointmentListFragment;


public class AppointmentActivity extends BaseActivity {

    // Runs security checks before allowing the AppointmentListFragment to load the appointments.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (RuntimeGuard.isEnvironmentUnsafe()) {
            Toast.makeText(this, "Application cannot run in this environment.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        boolean bypassRbac = getIntent().getBooleanExtra("bypassRbacCheck", false);

        if (!bypassRbac && !RbacPolicyEvaluator.canViewAppointments(this)) {
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
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.appointmentContainer, new AppointmentListFragment())
                                .commit();
//                        runOnUiThread(() -> {
//                            Toast.makeText(AppointmentActivity.this, "Authentication failed: " + reason, Toast.LENGTH_LONG).show();
//                            finish();
//                        });
                    }
                });
    }
}
