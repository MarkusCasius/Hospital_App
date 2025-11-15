package com.example.hospimanagmenetapp.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Class for managing background tasks in the application.

public class AppExecutors {
    private static final Object LOCK = new Object();
    private static AppExecutors sInstance;

    private final Executor diskIO;
    private final Executor networkIO;
    private final Executor mainThread;

    private AppExecutors(Executor diskIO, Executor networkIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    public static AppExecutors getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new AppExecutors(
                            Executors.newSingleThreadExecutor(), // A single thread for all DB operations to ensure they are serial.
                            Executors.newFixedThreadPool(3), // A pool of 3 threads for network requests.
                            new MainThreadExecutor() // A handler-based executor for posting to the UI thread.
                    );
                }
            }
        }
        return sInstance;
    }


    // Executor for database operations. Guarantees serial execution.
    public Executor diskIO() {
        return diskIO;
    }

    // Executor for network operations.
    public Executor networkIO() {
        return networkIO;
    }

    // Executor that posts to the main UI thread.
    public Executor mainThread() {
        return mainThread;
    }

    // An Executor that runs tasks on the main thread.
    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}

