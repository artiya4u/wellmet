package com.bootlegsoft.wellmet;

import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App extends Application {

    private static final Logger sLogger = LoggerFactory.getLogger(App.class);

    private static App sInstance;

    private ComponentName mRebootReceiverComponent;
    private PackageManager mPm;


    public static App getInstance() {
        return sInstance;
    }


    @Override
    public void onCreate() {
        sInstance = this;
        super.onCreate();
        sLogger.info("Beacon simulator starting!");

        mRebootReceiverComponent = new ComponentName(this, RebootReceiver.class);
        mPm = getPackageManager();

        startResilientBeacons();
    }


    public void startResilientBeacons() {
        IBeaconSimulatorService.startBroadcast(this);
    }


}
