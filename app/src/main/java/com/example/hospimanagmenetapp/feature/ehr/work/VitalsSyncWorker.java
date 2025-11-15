package com.example.hospimanagmenetapp.feature.ehr.work;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Vitals;
import java.util.List;

public class VitalsSyncWorker extends Worker {
    public VitalsSyncWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull @Override
    public Result doWork() {
        List<Vitals> pending = AppDatabase.getInstance(getApplicationContext())
                .vitalsDao().getPending();
        for (Vitals v : pending) {
            // TODO: simulate upload to server
            AppDatabase.getInstance(getApplicationContext())
                    .vitalsDao().markSynced(v.id);
        }
        return Result.success();
    }
}

