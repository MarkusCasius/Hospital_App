package com.example.hospimanagmenetapp.feature.ehr.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.example.hospimanagmenetapp.security.RuntimeGuard;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import androidx.appcompat.app.AppCompatActivity;

public class BarcodeScannerActivity extends AppCompatActivity {
    private DecoratedBarcodeView barcodeView;
    private Handler handler = new Handler();
    private boolean barcodeDetected = false;

    // The barcode scanner activity is between the main activity and the patient summary activity.
    // It opens an activity that allows the user to scan a QR code to pass through an NHS number
    // If there isn't a QR code to scan or camera isn't available, then it navigates to the patient summary activity
    // without passing through an NHS number.

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (RuntimeGuard.isEnvironmentUnsafe()) {
            Toast.makeText(this, "Application cannot run in this environment.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        barcodeView = new DecoratedBarcodeView(this);

        if (!RbacPolicyEvaluator.canViewEhr(this)) {
            Toast.makeText(this, "Access Denied. You do not have permission to view this page.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(barcodeView);
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (!barcodeDetected) {
                    barcodeDetected = true;
                    barcodeView.pause();
                    navigateToPatientSummary(result.getText());
                }
            }
        });

        // Timeout after 2 seconds if no barcode detected
        handler.postDelayed(() -> {
            if (!barcodeDetected) {
                barcodeView.pause();
                navigateToPatientSummary(null);
            }
        }, 2000);
    }

    private void navigateToPatientSummary(String nhsNumber) {
        Intent intent = new Intent(BarcodeScannerActivity.this, PatientSummaryActivity.class);
        intent.putExtra("nhsNumber", nhsNumber);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}

