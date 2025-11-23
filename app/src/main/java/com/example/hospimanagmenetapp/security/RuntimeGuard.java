package com.example.hospimanagmenetapp.security;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

public class RuntimeGuard {

    private static final String TAG = "RuntimeGuard";

    private static final String VALID_SIGNATURE_HASH = "7B:9A:BD:D6:F8:BC:7C:1E:39:A3:E6:B5:DD:0C:EB:63:90:51:B3:D4:50:6B:1F:EB:28:54:60:36:36:C3:65:F3E";

    private static boolean isRooted = false;
    private static boolean isTampered = false;

    public static void initialize(Context context) {
        if (checkRoot() || checkSuspiciousLibraries()) {
            isRooted = true;
            Log.e(TAG, "Security Alert: Device is rooted or has suspicious libraries.");
        }

        if (!checkSignature(context)) {
            isTampered = true;
            Log.e(TAG, "Security Alert: Application signature is invalid. The app has been tampered with.");
        }
    }

    public static boolean isEnvironmentUnsafe() {
        return isRooted || isTampered;
    }

    public static boolean isDebuggerAttached() {
        boolean isDebugging = android.os.Debug.isDebuggerConnected();
        if (isDebugging) {
            Log.w(TAG, "Security Warning: Debugger is attached.");
        }
        return isDebugging;
    }

    private static boolean checkSignature(Context context) {
        try {
            Signature[] signatures = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;

            if (signatures == null || signatures.length == 0) {
                return false;
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(signatures[0].toByteArray());
            byte[] digest = md.digest();

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String currentHash = hexString.toString().toUpperCase();

            Log.d(TAG, "Current Signature Hash: " + currentHash);

            return VALID_SIGNATURE_HASH.toUpperCase().equals(currentHash);
        } catch (Exception e) {
            Log.e(TAG, "Signature check failed.", e);
            return false;
        }
    }

    private static boolean checkRoot() {
        // Check for common root binaries
        String[] paths = {
                "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
                "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }

        // Check if `su` command can be executed
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) return true;
        } catch (Throwable t) {
            // This can happen if the command doesn't exist
        } finally {
            if (process != null) process.destroy();
        }

        // Check for "test-keys" build tag
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        return false;
    }

    private static boolean checkSuspiciousLibraries() {
        try {
            Set<String> loadedLibraries = new HashSet<>();
            String mapsFile = "/proc/" + android.os.Process.myPid() + "/maps";
            BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(mapsFile)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".so") || line.endsWith(".jar")) {
                    int lastSlash = line.lastIndexOf("/");
                    if (lastSlash != -1) {
                        loadedLibraries.add(line.substring(lastSlash + 1));
                    }
                }
            }
            reader.close();

            // Check for Frida and Xposed libraries
            for (String library : loadedLibraries) {
                if (library.contains("frida")) {
                    Log.e(TAG, "Security Alert: Frida library detected!");
                    return true;
                }
                if (library.contains("xposed")) {
                    Log.e(TAG, "Security Alert: Xposed library detected!");
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check loaded libraries.", e);
        }
        return false;
    }
}
