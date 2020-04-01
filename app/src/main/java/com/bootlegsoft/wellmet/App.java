package com.bootlegsoft.wellmet;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App extends Application {

    private static final Logger sLogger = LoggerFactory.getLogger(App.class);

    private static App sInstance;

    public static App getInstance() {
        return sInstance;
    }


    @Override
    public void onCreate() {
        sInstance = this;
        super.onCreate();
        sLogger.info("Beacon simulator starting!");

        startResilientBeacons();
    }


    public void startResilientBeacons() {
        IBeaconSimulatorService.startBroadcast(this);
    }


}
