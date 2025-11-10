package com.example.hospimanagmenetapp.ui; // UI layer package for Activities

import androidx.appcompat.app.AppCompatActivity; // Base class for Activities with AppCompat features

import android.content.Intent;  // For navigating between Activities
import android.os.Bundle;       // Lifecycle state bundle
import android.text.TextUtils;  // Simple string checks (e.g., isEmpty)
import android.widget.Button;   // UI widget: Button
import android.widget.EditText; // UI widget: text input
import android.widget.Toast;    // Lightweight on-screen notifications

import com.example.hospimanagmenetapp.R;                     // Resource references (layouts, IDs, strings)
import com.example.hospimanagmenetapp.data.AppDatabase;      // Room database singleton
import com.example.hospimanagmenetapp.data.dao.StaffDao;
import com.example.hospimanagmenetapp.data.entities.Staff;   // Staff entity (contains role and PIN)
import com.example.hospimanagmenetapp.util.EncryptionManager;
import com.example.hospimanagmenetapp.util.SessionManager;   // Simple session storage (SharedPreferences)

import java.util.List;
import java.util.concurrent.Executors; // Run DB work off the main thread

public class AdminLoginActivity extends AppCompatActivity { // Screen for admin authentication

    private EditText etEmail, etPin;        // Inputs for admin email/PIN
    private Button btnLogin, btnOpenSetup;  // Actions: login or open bootstrap/setup

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Called when the Activity is created
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login); // Inflate the admin login layout

        // Bind views
        etEmail = findViewById(R.id.etAdminEmail);
        etPin = findViewById(R.id.etAdminPin);
        btnLogin = findViewById(R.id.btnAdminLogin);
        btnOpenSetup = findViewById(R.id.btnOpenAdminSetup);

        // Attach handlers
        btnLogin.setOnClickListener(v -> doLogin());          // Attempt sign-in with provided email/PIN
        btnOpenSetup.setOnClickListener(v -> openAdminPortal()); // Allow bootstrap into Admin Portal (first admin registration)
    }

    @Override
    protected void onResume() { // Called when the Activity returns to foreground
        super.onResume();
        // Check if any ADMIN exists; if none, enable the setup button so you can create the first admin
        Executors.newSingleThreadExecutor().execute(() -> {
            int admins = AppDatabase.getInstance(getApplicationContext()).staffDao().countAdmins(); // Background DB call
            runOnUiThread(() -> btnOpenSetup.setEnabled(admins == 0)); // UI update must be on main thread
        });
    }

    // Navigate directly to the Admin Portal in "bootstrap" mode
    private void openAdminPortal() {
        Intent i = new Intent(this, AdminPortalActivity.class); // Intent to open Admin Portal
        i.putExtra("bypassCheck", true);           // Flag so the portal can skip normal prechecks for first-time setup
        startActivity(i);               // Launch the portal
        finish();               // Close login so back won’t return here
    }

    // Validate inputs and perform admin login using background DB lookup
    private void doLogin() {
        String email = etEmail.getText().toString().trim(); // Read/trim email
        String pin = etPin.getText().toString().trim();     // Read/trim PIN

        // Basic required-field checks
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pin)) {
            Toast.makeText(this, "Email and PIN are required.", Toast.LENGTH_SHORT).show();
            return; // Don’t proceed without both fields
        }

        // Run the lookup off the main thread (Room requirement / UI responsiveness)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                StaffDao dao = AppDatabase.getInstance(getApplicationContext()).staffDao();
                EncryptionManager encryptionManager = new EncryptionManager();
                // Fetch ALL staff members from the database.
                List<Staff> allStaff = dao.getAll();
                Staff matchedAdmin = null;

                // Loop through each staff member to find a match.
                for (Staff staff : allStaff) {
                    // Only consider admins
                    if (staff.role == Staff.Role.ADMIN) {
                        String decryptedEmail = null;
                        try {
                            // Decrypt the email for comparison.
                            decryptedEmail = encryptionManager.decrypt(staff.email);
                        } catch (Exception e) {
                            continue; // Skip this record if decryption fails
                        }

                        // If emails match, check the PIN.
                        if (email.equals(decryptedEmail)) {
                            String decryptedPin = encryptionManager.decrypt(staff.adminPin);
                            if(pin.equals(decryptedPin)) {
                                matchedAdmin = staff; // Found our user!
                                matchedAdmin.email = decryptedEmail; // Store the plaintext email for the session
                                break; // Exit the loop
                            }
                        }
                    }
                }

                // Check if we found a matching admin.
                if (matchedAdmin == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Invalid admin credentials.", Toast.LENGTH_SHORT).show()); // Show error on UI thread
                } else {
                    // Persist session details and proceed into the Admin Portal
                    final Staff finalAdmin = matchedAdmin;
                    runOnUiThread(() -> {
                        // Use the plaintext email for the session
                        SessionManager.setCurrentUser(this, "ADMIN", finalAdmin.email);
                        Intent i = new Intent(this, AdminPortalActivity.class); // Navigate to the portal proper
                        startActivity(i);
                        finish(); // Close login screen after success
                    });
                }
            } catch (Exception e) {
            // If decryption or database access fails, show an error
            runOnUiThread(() ->
                    Toast.makeText(this, "Failed to load and decrypt staff data.", Toast.LENGTH_LONG).show());
        }
        });
    }
}