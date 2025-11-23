package com.example.hospimanagmenetapp.security.auth;

import android.util.Log;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class BiometricLoginCoordinator {

    public interface Callback {
        void onSuccess();
        void onFailure(String reason);
    }

    // The Biometric login coordinator is used to validate user authenticity, employing
    // zero-trust architecture to help keep sensitive data private when navigating to
    // activities which display such (e.g. Patient Summary, Appointment)

    public void authenticate(FragmentActivity activity, Callback cb) {
        Log.d("BiometricAuth", "Starting biometric authentication");

        BiometricManager bm = BiometricManager.from(activity);
        int can = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        switch (can) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("BiometricAuth", "Biometric authentication is available");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e("BiometricAuth", "No biometric hardware available");
                cb.onFailure("No biometric hardware");
                return;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e("BiometricAuth", "Biometric hardware currently unavailable");
                cb.onFailure("Biometric hardware unavailable");
                return;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.e("BiometricAuth", "No biometric credentials enrolled");
                cb.onFailure("No biometrics enrolled");
                return;
            default:
                Log.e("BiometricAuth", "Unknown biometric error: " + can);
                cb.onFailure("Biometrics unavailable");
                return;
        }

        Executor ex = ContextCompat.getMainExecutor(activity);
        BiometricPrompt prompt = new BiometricPrompt(activity, ex,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        Log.d("BiometricAuth", "Authentication succeeded");
                        cb.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        Log.e("BiometricAuth", "Authentication error [" + errorCode + "]: " + errString);
                        cb.onFailure(errString.toString());
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        Log.w("BiometricAuth", "Authentication failed — biometric not recognized");
                        cb.onFailure("Authentication failed");
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm your identity")
                .setSubtitle("Access Appointments")
                .setNegativeButtonText("Cancel")
                .build();

        Log.d("BiometricAuth", "Prompting user for biometric authentication");
        prompt.authenticate(info);
    }
}

