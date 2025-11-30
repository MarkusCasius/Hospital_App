package com.example.hospimanagmenetapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.hospimanagmenetapp.security.RuntimeGuard;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;
import java.security.MessageDigest;

@RunWith(AndroidJUnit4.class)
public class RuntimeGuardTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Reset the static flags before each test.
        setStaticField("isRooted", false);
        setStaticField("isTampered", false);
    }

    /**
     * Helper method to modify private static fields via reflection for testing.
     */
    private void setStaticField(String fieldName, Object value) {
        try {
            Field field = RuntimeGuard.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set static field for test", e);
        }
    }

    @Test
    public void checkSignature_withValidDebugSignature_doesNotFlagAsTampered() throws Exception {
        // Arrange: Get the current valid debug hash
        Signature[] signatures = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        Assert.assertNotNull(signatures);
        md.update(signatures[0].toByteArray());
        byte[] digest = md.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String currentDebugHash = hexString.toString().toUpperCase();

        // Temporarily set the valid hash in RuntimeGuard
        setStaticField("VALID_SIGNATURE_HASH", currentDebugHash);

        // Act: Initialize the guard
        RuntimeGuard.initialize(context);

        // Assert: Use the PUBLIC method to check the result.
        assertFalse("Environment should be safe when signature is valid", RuntimeGuard.isEnvironmentUnsafe());
    }

    @Test
    public void checkSignature_withInvalidSignature_flagsAsTampered() {
        // Arrange: Set an obviously invalid hash
        setStaticField("VALID_SIGNATURE_HASH", "INVALID_HASH");

        // Act: Initialize the guard
        RuntimeGuard.initialize(context);

        // Assert: Use the PUBLIC method to check the result.
        assertTrue("Environment should be unsafe when signature is invalid", RuntimeGuard.isEnvironmentUnsafe());
    }

    @Test
    public void checkRoot_whenSuFileExists_flagsAsRooted() {
        // Arrange: Mock the file system to simulate a rooted device
        try (MockedStatic<File> fileMock = Mockito.mockStatic(File.class, Mockito.CALLS_REAL_METHODS)) {
            File suFile = mock(File.class);
            when(suFile.exists()).thenReturn(true);
            fileMock.when(() -> new File("/system/bin/su")).thenReturn(suFile);

            // Act: Initialize the guard
            RuntimeGuard.initialize(context);

            // Assert: Use the PUBLIC method to check the result.
            assertTrue("Environment should be unsafe when 'su' file is found", RuntimeGuard.isEnvironmentUnsafe());
        }
    }

    @Test
    public void isEnvironmentUnsafe_onCleanDevice_returnsFalse() {
        // Arrange: On a standard emulator, none of the root checks should trigger.
        try {
            Signature[] signatures = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            Assert.assertNotNull(signatures);
            md.update(signatures[0].toByteArray());
            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            setStaticField("VALID_SIGNATURE_HASH", hexString.toString().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Act: Initialize the guard
        RuntimeGuard.initialize(context);

        // Assert: The environment should be considered safe.
        assertFalse("Environment should be safe on a clean test device with a valid signature", RuntimeGuard.isEnvironmentUnsafe());
    }

    @Test
    public void isDebuggerAttached_whenRunningInTest_returnsTrue() {
        assertTrue("isDebuggerAttached should return true during a debug test run", RuntimeGuard.isDebuggerAttached());
    }
}