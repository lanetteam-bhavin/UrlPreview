package com.example.lcom53.urlpreview;

import android.app.Application;
import android.content.Context;

/**
 * @author ParthS
 * @since 20/1/16.
 */
public class GlobalApp extends Application {
    private static GlobalApp instance;

    public GlobalApp() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}