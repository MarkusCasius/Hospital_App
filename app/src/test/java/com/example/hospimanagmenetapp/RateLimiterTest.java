package com.example.hospimanagmenetapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.hospimanagmenetapp.security.RateLimiter;


public class RateLimiterTest {

    @Mock
    private Context mockContext;
    @Mock
    private SharedPreferences mockPrefs;
    @Mock
    private SharedPreferences.Editor mockEditor;

    private RateLimiter rateLimiter;

    @Before
    public void setUp() {
        // Mock the SharedPreferences behavior
        when(mockContext.getSharedPreferences(anyString(), any(Integer.class))).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);

        rateLimiter = new RateLimiter(mockContext);
    }

    @Test
    public void isAttemptAllowed_whenNoPreviousAttempts_shouldReturnTrue() {
        // Arrange: No previous timestamps exist
        when(mockPrefs.getStringSet(anyString(), any())).thenReturn(new HashSet<>());

        // Act & Assert
        assertTrue("Attempt should be allowed when there are no previous attempts", rateLimiter.isAttemptAllowed());
    }

    @Test
    public void isAttemptAllowed_whenUnderLimit_shouldReturnTrue() {
        // Arrange: 4 recent timestamps
        Set<String> timestamps = new HashSet<>();
        long now = System.currentTimeMillis();
        timestamps.add(String.valueOf(now - 1000));
        timestamps.add(String.valueOf(now - 2000));
        timestamps.add(String.valueOf(now - 3000));
        timestamps.add(String.valueOf(now - 4000));
        when(mockPrefs.getStringSet(anyString(), any())).thenReturn(timestamps);

        // Act & Assert
        assertTrue("Attempt should be allowed when under the limit", rateLimiter.isAttemptAllowed());
    }

    @Test
    public void isAttemptAllowed_whenAtLimit_shouldReturnFalse() {
        // Arrange: 5 recent timestamps (the max limit)
        Set<String> timestamps = new HashSet<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            timestamps.add(String.valueOf(now - (i * 1000)));
        }
        when(mockPrefs.getStringSet(anyString(), any())).thenReturn(timestamps);

        // Act & Assert
        assertFalse("Attempt should be denied when at the limit", rateLimiter.isAttemptAllowed());
    }
}

