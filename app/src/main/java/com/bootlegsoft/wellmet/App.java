package com.bootlegsoft.wellmet;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;
import java.util.UUID;


public class App extends Application implements BootstrapNotifier, BeaconConsumer {
    private static final String TAG = "App";

    public static final String BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    public static final String MAJOR = String.valueOf(0xC0);
    public static final String MINOR = String.valueOf(0x19);

    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "status";

    private BeaconManager beaconManager;
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private BeaconTransmitter beaconTransmitter;
    private boolean haveDetectedBeaconsSinceBoot = false;

    private static App sInstance;

    public static App getInstance() {
        return sInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // iBeacon
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_LAYOUT));

        BeaconParser beaconParser = new BeaconParser().setBeaconLayout(BEACON_LAYOUT);
        beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        start();
    }


    public static UUID getUUID() {
        return UUID.randomUUID(); // TODO Generate UUID from user phone number and hashing with date.
    }


    public void start() {
        updateNotification();
        startScan();
        startAdvertise();
    }

    public void stop() {
        stopScan();
        stopAdvertise();
    }

    private void startScan() {
        Log.d(TAG, "Starting scan of beacons");
        beaconManager.bind(this);
        Region region = new Region("backgroundRegion",
                null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);
    }

    private void stopScan() {
        beaconManager.unbind(this);
        if (regionBootstrap != null) {
            regionBootstrap.disable();
            regionBootstrap = null;
        }
    }

    private void startAdvertise() {
        Beacon beacon = new Beacon.Builder()
                .setId1(getUUID().toString())
                .setId2(MAJOR)
                .setId3(MINOR)
                .setManufacturer(0x004C)
                .setTxPower(-65)
                .build();

        beaconTransmitter.startAdvertising(beacon);
    }

    private void stopAdvertise() {
        beaconTransmitter.stopAdvertising();
    }


    @Override
    public void didEnterRegion(Region arg0) {
        // In this example, this class sends a notification to the user whenever a Beacon
        // matching a Region (defined above) are first seen.
        Log.d(TAG, "did enter region.");
        if (!haveDetectedBeaconsSinceBoot) {
            Log.d(TAG, "auto launching MainActivity");

            // The very first time since boot that we detect an beacon, we launch the
            // MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Important:  make sure to add android:launchMode="singleInstance" in the manifest
            // to keep multiple copies of this activity from getting created if the user has
            // already manually launched the app.
            this.startActivity(intent);
            haveDetectedBeaconsSinceBoot = true;
        } else {
            sendNotificationBeacon(arg0.getId1().toUuid().toString());
        }


    }

    @Override
    public void didExitRegion(Region region) {

    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {

    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    for (Beacon b : beacons) {
                        Log.i(TAG, "ID: " + b.getId1() +
                                " Major: " + b.getId2() +
                                " Minor: " + b.getId3() +
                                " Distance: " + b.getDistance() + " meters");
                        sendNotificationBeacon(b.getId1().toUuid().toString());
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("com.bootlegsoft.wellmet.rangingRegion", null, null, null));
        } catch (RemoteException e) {
        }
    }

    private void updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel creation
            final CharSequence name = getString(R.string.notif_channel_name);
            final String description = getString(R.string.notif_channel_description);
            final int importance = NotificationManager.IMPORTANCE_LOW;
            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(App.this, CHANNEL_ID);
        builder
                .setSmallIcon(R.drawable.ic_radio)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_message));

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);

        beaconManager.enableForegroundServiceScanning(builder.build(), NOTIFICATION_ID);

        // For the above foreground scanning service to be useful, you need to disable
        // JobScheduler-based scans (used on Android 8+) and set a fast background scan
        // cycle that would otherwise be disallowed by the operating system.
        //
        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setBackgroundBetweenScanPeriod(0);
        beaconManager.setBackgroundScanPeriod(1100);
    }

    public void sendNotificationBeacon(String uuid) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_radio)
                .setContentTitle("Someone is nearby.")
                .setContentText(uuid)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(uuid.hashCode(), builder.build());
    }


}
