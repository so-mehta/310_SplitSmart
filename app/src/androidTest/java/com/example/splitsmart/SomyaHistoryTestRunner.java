package com.example.splitsmart;

import android.app.Application;
import android.content.Context;
import androidx.test.runner.AndroidJUnitRunner;

/** Opt-in runner: only Somya's isolated history UI suite uses this application. */
public class SomyaHistoryTestRunner extends AndroidJUnitRunner {
    @Override public Application newApplication(ClassLoader loader, String name, Context context)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(loader, SomyaHistoryTestApplication.class.getName(), context);
    }
}
