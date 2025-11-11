package com.example.hospimanagmenetapp.util; // Utility classes (non-UI helpers) live here

import android.content.Context;           // Needed to obtain SharedPreferences
import android.content.SharedPreferences; // Key–value storage for simple app state

public class SessionManager { // Simple wrapper around SharedPreferences for session data

    private static final String PREF = "hms_prefs";          // Preferences file name
    private static final String KEY_ROLE = "current_role";   // Key for stored user role
    private static final String KEY_IDENTIFIER = "current_identifier"; // Key for stored user email

    public static void setCurrentUser(Context ctx, String role, String email) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE); // Private prefs for this app only
        sp.edit()    // Begin an edit transaction
                .putString(KEY_ROLE, role)             // Save the user’s role (e.g., "ADMIN", "STAFF")
                .putString(KEY_IDENTIFIER, email)           // Save the user’s email address/ID
                .apply();     // Apply asynchronously (non-blocking)
    }

    public static String getCurrentRole(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE); // Open prefs
        return sp.getString(KEY_ROLE, ""); // Return role or empty string if none set
    }

    public static String getCurrentIdentifier(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE); // Open prefs
        return sp.getString(KEY_IDENTIFIER, ""); // Return email/ID or empty string if none set
    }

    public static void clear(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE); // Open prefs
        sp.edit().clear().apply(); // Remove all stored keys for a full “logout/reset”
    }
}