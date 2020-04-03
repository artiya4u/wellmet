package com.bootlegsoft.wellmet;

import android.app.Application;
import android.util.Log;

public class App extends Application {
    private static final String TAG = ".WellMet";
    private static App sInstance;

    public static App getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Beacon simulator starting!");

        startResilientBeacons();
    }


    public void startResilientBeacons() {
        IBeaconService.start(this);
    }


}
