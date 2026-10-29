package com.example.splitsmart;

import android.app.Application;

/**
 * This is the only class allowed to import both the repository implementations
 * and the services
 */
public class SplitSmartApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Firebase is initialized automatically by the google-services plugin.
    }
}
