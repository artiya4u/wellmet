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

    public void enableRebootResilience(boolean enable) {
        final int componentState = mPm.getComponentEnabledSetting(mRebootReceiverComponent);
        boolean lastState = false;
        switch (componentState) {
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                lastState = true;
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT: // Shoud be explicitely disabled by default in the manifest
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                lastState = false;
                break;
            default:
        }
        if (lastState == enable) {
            return;
        }
        final int status = enable
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        mPm.setComponentEnabledSetting(mRebootReceiverComponent, status, PackageManager.DONT_KILL_APP);
        sLogger.info("Setting broadcast resilience to: {}", enable);
    }


}
