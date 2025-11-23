package com.example.hospimanagmenetapp.feature.ehr.work;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.hospimanagmenetapp.data.entities.Vitals;
import com.example.hospimanagmenetapp.data.repo.EhrRepository;
import java.util.List;

public class VitalsSyncWorker extends Worker {
    private static final String TAG = "VitalsSyncWorker";

    // Background sync worker for syncing the vitals with the database periodically to ensure
    // data consistency and accuracy.

    public VitalsSyncWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull @Override
    public Result doWork() {
        EhrRepository repository = new EhrRepository(getApplicationContext());
        List<Vitals> pendingVitals = repository.getUnsyncedVitals();
        Log.d(TAG, "Found " + pendingVitals.size() + " pending vitals to sync.");

        if (pendingVitals.isEmpty()) {
            return Result.success();
        }

        boolean allSucceeded = true;
        for (Vitals v : pendingVitals) {
            Log.d(TAG, "Syncing vitals ID: " + v.id);
            boolean success = repository.syncVitals(v);
            if (!success) {
                allSucceeded = false;
                Log.w(TAG, "Failed to sync vitals ID: " + v.id);
            }
        }

        return allSucceeded ? Result.success() : Result.retry();
    }
}
