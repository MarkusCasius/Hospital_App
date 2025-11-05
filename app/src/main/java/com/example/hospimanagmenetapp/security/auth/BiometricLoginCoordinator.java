package com.example.hospimanagmenetapp.security.auth;

import android.app.Activity;
import android.content.Context;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class BiometricLoginCoordinator {

    public interface Callback {
        void onSuccess();
        void onFailure(String reason);
    }

    public void authenticate(Activity activity, Callback cb) {
        BiometricManager bm = BiometricManager.from(activity);
        int can = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            cb.onFailure("Biometrics unavailable");
            return;
        }

        Executor ex = ContextCompat.getMainExecutor(activity);
        BiometricPrompt prompt = new BiometricPrompt(activity, ex,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        cb.onSuccess();
                    }
                    @Override public void onAuthenticationError(int errorCode, CharSequence errString) {
                        cb.onFailure(errString.toString());
                    }
                    @Override public void onAuthenticationFailed() {
                        cb.onFailure("Authentication failed");
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm your identity")
                .setSubtitle("Access Appointments")
                .setNegativeButtonText("Cancel")
                .build();

        prompt.authenticate(info);
    }
}
