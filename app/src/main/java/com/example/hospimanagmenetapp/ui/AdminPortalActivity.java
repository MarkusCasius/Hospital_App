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

import com.example.hospimanagmenetapp.R;                     // Resource references (layouts, IDs)
import com.example.hospimanagmenetapp.data.AppDatabase;      // Room database singleton
import com.example.hospimanagmenetapp.data.dao.StaffDao;     // DAO for Staff operations
import com.example.hospimanagmenetapp.data.entities.Staff;   // Staff entity (has Role enum, email, PIN)
import com.example.hospimanagmenetapp.ui.adapters.StaffAdapter; // RecyclerView adapter to render staff list
import com.example.hospimanagmenetapp.util.EncryptionManager;
import com.example.hospimanagmenetapp.util.SessionManager;   // Simple session storage for RBAC checks

import java.util.ArrayList;
import java.util.Arrays;             // Utility to turn arrays into Lists
import java.util.List;               // List interface for collections
import java.util.concurrent.Executors; // Run DB work off the main thread

import javax.crypto.EncryptedPrivateKeyInfo;

public class AdminPortalActivity extends AppCompatActivity { // Admin portal: manage staff accounts

    private EditText etName, etEmail, etPin;   // Inputs for staff name/email and admin PIN (if role is ADMIN)
    private Spinner spRole;                    // Role picker (ADMIN/STAFF/etc.)
    private Button btnRegisterStaff, btnRefresh; // Actions to register and refresh the list
    private RecyclerView rvStaff;              // Displays the current staff members
    private static final String TAG = "PatientLoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Activity creation lifecycle
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_portal);  // Inflate the admin portal layout

        // Bind views from XML
        etName = findViewById(R.id.etStaffName);
        etEmail = findViewById(R.id.etStaffEmail);
        etPin = findViewById(R.id.etAdminSetupPin);
        spRole = findViewById(R.id.spRole);
        btnRegisterStaff = findViewById(R.id.btnRegisterStaff);
        btnRefresh = findViewById(R.id.btnRefreshList);
        rvStaff = findViewById(R.id.rvStaff);

        rvStaff.setLayoutManager(new LinearLayoutManager(this)); // Vertical list for the RecyclerView

        // Populate the role Spinner with all Staff.Role enum values using a simple built-in layout
        spRole.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList(Staff.Role.values())
        ));

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

    // Read inputs, validate, and insert a new Staff record (background thread)
    private void registerStaff() {
        String name = etName.getText().toString().trim();     // Staff full name
        String email = etEmail.getText().toString().trim();   // Staff email (should be unique)
        Staff.Role role = (Staff.Role) spRole.getSelectedItem(); // Selected role from Spinner
        String pin = etPin.getText().toString().trim();       // Admin PIN (required only for ADMIN)

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

                List<Staff> allStaff = dao.getAll();
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

                String encryptedName = encryptionManager.encrypt(name);
                String encryptedEmail = encryptionManager.encrypt(email);
                String encryptedPin = encryptionManager.encrypt(pin);

                Staff s = new Staff();          // Create new entity
                s.fullName = encryptedName;              // Map inputs to fields
                s.email = encryptedEmail;
                s.role = role;
                s.adminPin = (role == Staff.Role.ADMIN) ? encryptedPin : null; // Store PIN only for admins

                dao.insert(s); // Persist to Room (unique constraints may throw)

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
                List<Staff> encryptedList = AppDatabase.getInstance(getApplicationContext()).staffDao().getAll();
                EncryptionManager encryptionManager = new EncryptionManager();
                List<Staff> decryptedList = new ArrayList<>();

                for (Staff encryptedStaff : encryptedList) {
                    Staff decryptedStaff = new Staff();
                    decryptedStaff.id = encryptedStaff.id;
                    decryptedStaff.role = encryptedStaff.role;
                    decryptedStaff.adminPin = encryptedStaff.adminPin;

                    decryptedStaff.fullName = encryptionManager.decrypt(encryptedStaff.fullName);
                    decryptedStaff.email = encryptionManager.decrypt(encryptedStaff.email);


                    decryptedList.add(decryptedStaff);
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
                EncryptionManager encryptionManager = new EncryptionManager();
                Staff staffToDelete = new Staff();
                staffToDelete.id = staff.id; // The primary key
                staffToDelete.fullName = encryptionManager.encrypt(staff.fullName);
                staffToDelete.email = encryptionManager.encrypt(staff.email);
                staffToDelete.role = staff.role;
                staffToDelete.adminPin = staff.adminPin != null ? encryptionManager.encrypt(staff.adminPin) : null;

                AppDatabase.getInstance(getApplicationContext()).staffDao().delete(staffToDelete);

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