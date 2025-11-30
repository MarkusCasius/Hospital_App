package com.example.hospimanagmenetapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.hospimanagmenetapp.MainActivity;
import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.feature.ehr.ui.BarcodeScannerActivity;
import com.example.hospimanagmenetapp.feature.ehr.ui.PatientSummaryActivity;
import com.example.hospimanagmenetapp.util.SessionManager;
import com.google.android.material.navigation.NavigationView;

public abstract class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        // This is the base layout that includes the drawer.
        // The activity's actual content will be inflated into the 'content_frame'.
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        FrameLayout contentFrame = drawerLayout.findViewById(R.id.content_frame);

        getLayoutInflater().inflate(layoutResID, contentFrame, true);
        super.setContentView(drawerLayout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up icon to open and close the drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set user email in the header
        View headerView = navigationView.getHeaderView(0);
        TextView userEmailTextView = headerView.findViewById(R.id.nav_header_user_email);
        userEmailTextView.setText(SessionManager.getCurrentIdentifier(this));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent = null;

        int id = item.getItemId();
        if (id == R.id.nav_home) {
            intent = new Intent(this, MainActivity.class);
            // Clear the activity stack and start fresh
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (id == R.id.nav_appointments) {
            intent = new Intent(this, AppointmentActivity.class);
        } else if (id == R.id.nav_patient_summary) {
            intent = new Intent(this, BarcodeScannerActivity.class);
        } else if (id == R.id.nav_register_patient) {
            intent = new Intent(this, PatientRegistrationActivity.class);
        } else if (id == R.id.nav_admin_portal) {
            intent = new Intent(this, AdminPortalActivity.class);
        } else if (id == R.id.nav_logout) {
            SessionManager.clear(this);
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        // Close the drawer before starting the new activity
        drawerLayout.closeDrawer(GravityCompat.START);

        // Start the new activity after a small delay to allow the drawer to close smoothly
        if (intent != null) {
            // Prevent starting an activity if it's already the current one
            if (!this.getClass().getName().equals(intent.getComponent().getClassName())) {
                startActivity(intent);
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // If the drawer is open, close it on back press. Otherwise, perform default back action.
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
