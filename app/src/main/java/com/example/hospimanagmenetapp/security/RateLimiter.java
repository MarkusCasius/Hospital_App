package com.example.hospimanagmenetapp.security;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RateLimiter {

    // A class used to ensure that a maximum of 5 bookings are made within a 10-minute window, to
    // avoid DDOS attacks and denying patients for getting their appointments.

    private static final String PREFS_NAME = "RateLimitPrefs";
    private static final String KEY_APPOINTMENT_TIMESTAMPS = "appointmentTimestamps";

    // Allow a maximum of 5 bookings within a 10-minute window.
    private static final int MAX_ATTEMPTS = 5;
    private static final long TIME_WINDOW_MS = 10 * 60 * 1000; // 10 minutes in milliseconds

    private final SharedPreferences prefs;

    public RateLimiter(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAttemptAllowed() {
        long now = System.currentTimeMillis();
        long windowStart = now - TIME_WINDOW_MS;

        // Retrieve the set of timestamps.
        Set<String> timestampsStr = prefs.getStringSet(KEY_APPOINTMENT_TIMESTAMPS, new HashSet<>());
        Set<String> newTimestamps = new HashSet<>(timestampsStr);

        // Prune old timestamps and check the count of recent ones.
        Set<Long> recentTimestamps = newTimestamps.stream()
                .map(Long::parseLong)
                .filter(timestamp -> timestamp >= windowStart)
                .collect(Collectors.toSet());

        // If the number of recent attempts is less than the max, it's allowed.
        return recentTimestamps.size() < MAX_ATTEMPTS;
    }

    public void recordNewAttempt() {
        long now = System.currentTimeMillis();
        long windowStart = now - TIME_WINDOW_MS;

        Set<String> timestampsStr = prefs.getStringSet(KEY_APPOINTMENT_TIMESTAMPS, new HashSet<>());
        Set<String> newTimestamps = new HashSet<>(timestampsStr);

        // Prune old timestamps before adding the new one.
        Set<String> recentTimestampsStr = newTimestamps.stream()
                .filter(s -> Long.parseLong(s) >= windowStart)
                .collect(Collectors.toSet());

        // Add the new timestamp.
        recentTimestampsStr.add(String.valueOf(now));

        // Save the updated set back to SharedPreferences.
        prefs.edit().putStringSet(KEY_APPOINTMENT_TIMESTAMPS, recentTimestampsStr).apply();
    }
}