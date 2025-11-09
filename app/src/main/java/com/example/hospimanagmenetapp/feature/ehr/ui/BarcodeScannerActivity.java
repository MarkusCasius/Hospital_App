package com.example.hospimanagmenetapp.feature.ehr.ui;

import android.content.Intent;
import android.os.Bundle;

import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.google.zxing.Result;
import androidx.appcompat.app.AppCompatActivity;

public class BarcodeScannerActivity extends AppCompatActivity {
    private DecoratedBarcodeView barcodeView;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        barcodeView = new DecoratedBarcodeView(this);
        setContentView(barcodeView);
        barcodeView.decodeContinuous(new BarcodeCallback() {

            @Override public void barcodeResult(Result result) {
                barcodeView.pause();
                Intent i = new Intent(BarcodeScannerActivity.this, PatientSummaryActivity.class);
                i.putExtra("nhsNumber", result.getText());
                startActivity(i);
                finish();
            }
        });
    }
}

