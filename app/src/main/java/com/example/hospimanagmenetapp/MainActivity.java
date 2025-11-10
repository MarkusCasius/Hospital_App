package com.example.hospimanagmenetapp; // App's root package

import androidx.appcompat.app.AppCompatActivity; // Base class for modern Activities with ActionBar support

import android.content.Intent;  // Used to navigate between Activities
import android.os.Bundle;       // Holds saved instance state for lifecycle
import android.widget.Button;   // UI widget: Button
import android.widget.TextView; // UI widget: TextView

import com.example.hospimanagmenetapp.ui.AdminLoginActivity;        // Screen for admin sign-in
import com.example.hospimanagmenetapp.ui.AdminPortalActivity;       // Screen for admin features (opened after login)
import com.example.hospimanagmenetapp.ui.PatientRegistrationActivity; // Screen to register patients
import com.example.hospimanagmenetapp.util.SessionManager;          // Helper for simple session storage

public class MainActivity extends AppCompatActivity { // Entry Activity shown at app launch

    private TextView tvWelcome;       // Header showing session state
    private Button btnPatientRegistration, btnAdminPortal, btnLogout; // Main menu buttons

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Lifecycle: called when Activity is created
        super.onCreate(savedInstanceState);              // Always call the superclass first
        setContentView(R.layout.activity_main);          // Inflate the layout defined in activity_main.xml

        // Bind views from the layout to fields
        tvWelcome = findViewById(R.id.tvWelcome);
        btnPatientRegistration = findViewById(R.id.btnPatientRegistration);
        btnAdminPortal = findViewById(R.id.btnAdminPortal);
        btnLogout = findViewById(R.id.btnLogout);

        refreshHeader(); // Show current sign-in state immediately

        // Navigate to the Patient Registration screen
        btnPatientRegistration.setOnClickListener(v ->
                startActivity(new Intent(this, PatientRegistrationActivity.class)));

        // Navigate to Admin login first (RBAC gate). AdminPortal follows after successful login.
        btnAdminPortal.setOnClickListener(v -> {
            // RBAC gate: ADMIN only. If no admin exists yet, AdminLogin can handle bootstrap.
            Intent i = new Intent(this, AdminLoginActivity.class);
            startActivity(i);
        });

        // Clear session and update the header (acts like a simple "log out")
        btnLogout.setOnClickListener(v -> {
            SessionManager.clear(this); // Remove stored role/email
            refreshHeader();            // Reflect the logged-out state in the UI
        });
    }

    // Update the welcome header with the current session info
    private void refreshHeader() {
        String role = SessionManager.getCurrentRole(this);   // Read stored role (e.g., "ADMIN")
        String email = SessionManager.getCurrentEmail(this); // Read stored email
        if (role == null || role.isEmpty()) {                // No session present
            tvWelcome.setText("Welcome (not signed in)");    // Guest view
        } else {
            tvWelcome.setText("Signed in: " + email + " (" + role + ")"); // Show who is signed in
        }
    }

    @Override
    protected void onResume() { // Lifecycle: called when Activity comes to the foreground
        super.onResume();       // Always call the superclass
        refreshHeader();        // Re-read session in case it changed while away
    }
}