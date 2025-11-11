package com.example.hospimanagmenetapp.security.auth;

//import android.widget.Toast;
//
//import androidx.biometric.BiometricManager;
//import androidx.biometric.BiometricPrompt;
//import androidx.core.content.ContextCompat;
//import androidx.fragment.app.FragmentActivity;
//
//import com.example.hospimanagmenetapp.util.SessionManager;
//
//import java.util.concurrent.Executor;
//import java.util.function.Consumer;
//
//public class BiometricLoginCoordinator {
//
//    // Callback to pass the logged-in user's email back to the Activity.
//    public interface BiometricLoginCallback {
//        void onLoginSuccess(String userEmail);
//    }
//
//    public interface AuthenticationCallback {
//        void onAuthenticationSuccess();
//        void onAuthenticationFailed(String reason);
//    }
//
//    private final FragmentActivity activity;
//    private final BiometricLoginCallback loginCallback;
//
//    // Overloaded constructor for simple authentication
//    public BiometricLoginCoordinator(FragmentActivity activity) {
//        this.activity = activity;
//        this.loginCallback = null; // Not used in this mode
//    }
//
//    // Original constructor for the full login flow
//    public BiometricLoginCoordinator(FragmentActivity activity, BiometricLoginCallback loginCallback) {
//        this.activity = activity;
//        this.loginCallback = loginCallback;
//    }
//
//    public void authenticate(AuthenticationCallback callback) {
//        checkBiometricSupport(isSupported -> {
//            if (!isSupported) {
//                callback.onAuthenticationFailed("Biometrics not available or not set up.");
//                return;
//            }
//
//            Executor executor = ContextCompat.getMainExecutor(activity);
//            BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
//                    new BiometricPrompt.AuthenticationCallback() {
//                        @Override
//                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
//                            super.onAuthenticationSucceeded(result);
//                            callback.onAuthenticationSuccess(); // Simple success signal
//                        }
//
//                        @Override
//                        public void onAuthenticationError(int errorCode, CharSequence errString) {
//                            super.onAuthenticationError(errorCode, errString);
//                            if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
//                                callback.onAuthenticationFailed(errString.toString());
//                            } else {
//                                // User cancelled, so we also treat it as a failure to proceed
//                                callback.onAuthenticationFailed("User canceled authentication.");
//                            }
//                        }
//                    });
//
//            // Generic prompt for re-authentication
//            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
//                    .setTitle("Confirm Your Identity")
//                    .setSubtitle("Biometric authentication is required to proceed.")
//                    .setNegativeButtonText("Cancel")
//                    .build();
//
//            biometricPrompt.authenticate(promptInfo);
//        });
//    }
//
//
//    /**
//     * Checks if biometric hardware is available, enrolled, and permitted.
//     * The result is passed asynchronously to the consumer.
//     *
//     * @param isSupportedConsumer A consumer that accepts a boolean: true if biometrics can be used.
//     */
//    public void checkBiometricSupport(Consumer<Boolean> isSupportedConsumer) {
//        BiometricManager biometricManager = BiometricManager.from(activity);
//        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
//        // The result is true only if BIOMETRIC_SUCCESS is returned.
//        isSupportedConsumer.accept(canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS);
//    }
//
//    /**
//     * Initiates the biometric authentication prompt.
//     */
//    public void startAuthentication() {
//        // First, check if biometrics are actually supported before showing the prompt.
//        checkBiometricSupport(isSupported -> {
//            if (!isSupported) {
//                Toast.makeText(activity, "Biometrics not available or not set up.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Get the last logged-in user's email to display in the prompt.
//            String lastUserEmail = SessionManager.getCurrentEmail(activity);
//            if (lastUserEmail == null || lastUserEmail.isEmpty()) {
//                Toast.makeText(activity, "No previous admin login to use biometrics.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            Executor executor = ContextCompat.getMainExecutor(activity);
//            BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
//                    new BiometricPrompt.AuthenticationCallback() {
//                        @Override
//                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
//                            super.onAuthenticationSucceeded(result);
//                            // On success, invoke the callback with the last user's email.
//                            loginCallback.onLoginSuccess(lastUserEmail);
//                        }
//
//                        @Override
//                        public void onAuthenticationError(int errorCode, CharSequence errString) {
//                            super.onAuthenticationError(errorCode, errString);
//                            // Don't show a toast if the user simply canceled the prompt.
//                            if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
//                                Toast.makeText(activity, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
//                            }
//                        }
//
//                        @Override
//                        public void onAuthenticationFailed() {
//                            super.onAuthenticationFailed();
//                            // This callback is triggered when the fingerprint/face is not recognized.
//                            // The system handles retries, so we don't need to show a message here.
//                        }
//                    });
//
//            // Configure and show the prompt.
//            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
//                    .setTitle("Admin Login")
//                    .setSubtitle("Log in as " + lastUserEmail)
//                    .setNegativeButtonText("Cancel")
//                    .build();
//
//            biometricPrompt.authenticate(promptInfo);
//        });
//    }
//}










import android.app.Activity;
import android.content.Context;
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

