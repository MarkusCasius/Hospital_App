package com.example.hospimanagmenetapp.ui; // UI layer package for Activities

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;            // Base Activity with AppCompat features
import androidx.recyclerview.widget.LinearLayoutManager;    // Lays out RecyclerView items in a vertical list
import androidx.recyclerview.widget.RecyclerView;           // Efficient scrolling list/grid container

import android.os.Bundle;          // Lifecycle state bundle
import android.text.TextUtils;     // Simple string checks (e.g., isEmpty)
import android.util.Log;
import android.widget.ArrayAdapter; // Adapter to back the Spinner with enum values
import android.widget.Button;      // UI widget: Button
import android.widget.EditText;    // UI widget: text input
import android.widget.Spinner;     // UI widget: drop-down selection
import android.widget.Toast;       // Lightweight user notifications
import android.view.View;
import android.widget.AdapterView;

import com.example.hospimanagmenetapp.R;                     // Resource references (layouts, IDs)
import com.example.hospimanagmenetapp.data.AppDatabase;      // Room database singleton
import com.example.hospimanagmenetapp.data.dao.StaffDao;     // DAO for Staff operations
import com.example.hospimanagmenetapp.data.entities.Staff;   // Staff entity (has Role enum, email, PIN)
import com.example.hospimanagmenetapp.data.repo.StaffRepository;
import com.example.hospimanagmenetapp.security.RuntimeGuard;
import com.example.hospimanagmenetapp.ui.adapters.StaffAdapter; // RecyclerView adapter to render staff list
import com.example.hospimanagmenetapp.security.EncryptionManager;
import com.example.hospimanagmenetapp.util.SessionManager;   // Simple session storage for RBAC checks

import java.util.ArrayList;
import java.util.Arrays;             // Utility to turn arrays into Lists
import java.util.List;               // List interface for collections
import java.util.concurrent.Executors; // Run DB work off the main thread


public class AdminPortalActivity extends AppCompatActivity { // Admin portal: manage staff accounts

    private StaffRepository staffRepository;
    private EditText etName, etEmail, etPin;   // Inputs for staff name/email and admin PIN (if role is ADMIN)
    private Spinner spRole, spExpertise;                    // Role picker (ADMIN/STAFF/etc.)
    private Button btnRegisterStaff, btnRefresh; // Actions to register and refresh the list
    private RecyclerView rvStaff;              // Displays the current staff members
    private static final String TAG = "PatientLoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Activity creation lifecycle
        super.onCreate(savedInstanceState);


        if (RuntimeGuard.isEnvironmentUnsafe()) {
            Toast.makeText(this, "Application cannot run in this environment.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_portal);  // Inflate the admin portal layout
        staffRepository = new StaffRepository(this); // Initialize repository

        // Bind views from XML
        etName = findViewById(R.id.etStaffName);
        etEmail = findViewById(R.id.etStaffEmail);
        etPin = findViewById(R.id.etAdminSetupPin);
        spRole = findViewById(R.id.spRole);
        spExpertise = findViewById(R.id.spExpertise);
        btnRegisterStaff = findViewById(R.id.btnRegisterStaff);
        btnRefresh = findViewById(R.id.btnRefreshList);
        rvStaff = findViewById(R.id.rvStaff);

        rvStaff.setLayoutManager(new LinearLayoutManager(this)); // Vertical list for the RecyclerView

        spRole.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList(Staff.Role.values())
        ));

        spExpertise.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList(Staff.Expertise.values())
        ));

        spRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Staff.Role selectedRole = (Staff.Role) parent.getItemAtPosition(position);
                handleRoleChange(selectedRole);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Hide both conditional fields
                spExpertise.setVisibility(View.GONE);
                etPin.setVisibility(View.GONE);
            }
        });
        handleRoleChange((Staff.Role) spRole.getSelectedItem());

        // Wire up button actions
        btnRegisterStaff.setOnClickListener(v -> registerStaff()); // Validate inputs and insert staff
        btnRefresh.setOnClickListener(v -> loadStaff());           // Reload the staff list from DB

        // RBAC guard unless explicitly bypassed for first-admin bootstrap
        boolean bypass = getIntent().getBooleanExtra("bypassCheck", false); // True if launched from setup flow
        if (!bypass) {
            String role = SessionManager.getCurrentRole(this); // Read stored role
            if (!"ADMIN".equals(role)) {     // Only admins may enter
                Toast.makeText(this, "Admin access required.", Toast.LENGTH_SHORT).show();
                finish();        // Close and return to previous screen
                return;
            }
        }

        loadStaff(); // Populate list on first load
    }

    private void handleRoleChange(Staff.Role role) {
        if (role == Staff.Role.CLINICIAN) {
            spExpertise.setVisibility(View.VISIBLE);
            etPin.setVisibility(View.GONE);
        } else if (role == Staff.Role.ADMIN) {
            spExpertise.setVisibility(View.GONE);
            etPin.setVisibility(View.VISIBLE);
        } else {
            spExpertise.setVisibility(View.GONE);
            etPin.setVisibility(View.GONE);
        }
    }

    // Read inputs, validate, and insert a new Staff record (background thread)
    private void registerStaff() {
        String name = etName.getText().toString().trim();     // Staff full name
        String email = etEmail.getText().toString().trim();   // Staff email (should be unique)
        Staff.Role role = (Staff.Role) spRole.getSelectedItem(); // Selected role from Spinner
        String pin = etPin.getText().toString().trim();       // Admin PIN (required only for ADMIN)
        Staff.Expertise expertise = (Staff.Expertise) spExpertise.getSelectedItem();

        // Basic required-field checks
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Name and email are required.", Toast.LENGTH_SHORT).show();
            return; // Don’t proceed without essentials
        }
        // Enforce a PIN for ADMIN role
        if (role == Staff.Role.ADMIN && TextUtils.isEmpty(pin)) {
            Toast.makeText(this, "Admin PIN is required for ADMIN role.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Do DB I/O off the main thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                EncryptionManager encryptionManager = new EncryptionManager();
                StaffDao dao = AppDatabase.getInstance(getApplicationContext()).staffDao();

                List<Staff> allStaff = staffRepository.getAndCacheAllStaff();
                for (Staff staffMember : allStaff) {
                    try {
                        String decryptedEmail = encryptionManager.decrypt(staffMember.email);
                        if (email.equalsIgnoreCase(decryptedEmail)) {
                            // A staff member with this email already exists.
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Error: A staff member with this email already exists.", Toast.LENGTH_LONG).show());
                            return; // Stop the registration process.
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to decrypt email for staff ID: " + staffMember.id, e);
                    }
                }

                Staff s = new Staff();          // Create new entity
                s.fullName = name;
                s.email = email;
                s.role = role;
                s.adminPin = (role == Staff.Role.ADMIN) ? pin : null; // Store PIN only for admins
                s.expertise = (role == Staff.Role.CLINICIAN) ? expertise : null; // Store expertise only for clinicians

                staffRepository.registerStaff(EncryptionManager.encryptStaff(s));

                // On success, clear form and refresh list on the UI thread
                runOnUiThread(() -> {
                    Toast.makeText(this, "Staff registered.", Toast.LENGTH_SHORT).show();
                    etName.setText("");  // Reset inputs
                    etEmail.setText("");
                    etPin.setText("");
                    loadStaff();         // Refresh the RecyclerView with latest data
                });
            } catch (Exception e) { // Likely a uniqueness violation on email (if enforced)
                Log.e(TAG, "Error registering staff", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: email may already exist.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Fetch all staff from the DB and display them in the RecyclerView
    private void loadStaff() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Fetch all staff from the database
                List<Staff> encryptedList = staffRepository.getAndCacheAllStaff();
                List<Staff> decryptedList = new ArrayList<>();

                for (Staff encryptedStaff : encryptedList) {
                    decryptedList.add(EncryptionManager.decryptStaff(encryptedStaff));
                }

                // Pass the list to the adapter on the UI thread
                runOnUiThread(() -> {// The activity will need to implement StaffAdapter.OnStaffClickListener
                    StaffAdapter adapter = new StaffAdapter(decryptedList, this::showDeleteConfirmation);
                    rvStaff.setAdapter(adapter);
                });

            } catch (Exception e) {
                // If decryption or database access fails, show an error
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to load and decrypt staff data.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showDeleteConfirmation(final Staff staff) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Staff Member")
                .setMessage("Are you sure you want to delete " + staff.fullName + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteStaff(staff))
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    private void deleteStaff(final Staff staff) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Re-encrypt the identifying fields before deleting, as the DAO expects an encrypted object
                Staff staffToDelete = EncryptionManager.encryptStaff(staff);
                staffRepository.deleteStaff(EncryptionManager.encryptStaff(staff));

                // On success, show a toast and refresh the list
                runOnUiThread(() -> {
                    Toast.makeText(this, "Staff member deleted.", Toast.LENGTH_SHORT).show();
                    loadStaff(); // Refresh the RecyclerView
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error deleting staff member.", Toast.LENGTH_SHORT).show());
            }
        });
    }
}