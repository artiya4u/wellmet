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
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;
import java.util.UUID;


public class App extends Application implements BeaconConsumer {
    private static final String TAG = "App";

    public static final String BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    // For identify COVID-19 beacons.
    public static final int MAJOR = 0xC0;
    public static final int MINOR = 0x19;

    public static final float MINIMUM_DISTANCE = 1.0f;

    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "status";

    private BeaconManager beaconManager;
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private BeaconTransmitter beaconTransmitter;

    private static App sInstance;

    public static App getInstance() {
        return sInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // iBeacon parser
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_LAYOUT));
        BeaconParser beaconParser = new BeaconParser().setBeaconLayout(BEACON_LAYOUT);
        beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        backgroundPowerSaver = new BackgroundPowerSaver(this);
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
                .setId2(String.valueOf(MAJOR))
                .setId3(String.valueOf(MINOR))
                .setManufacturer(0x004C)
                .setTxPower(-65)
                .build();

        beaconTransmitter.startAdvertising(beacon);
    }

    private void stopAdvertise() {
        beaconTransmitter.stopAdvertising();
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
                        if (b.getDistance() <= MINIMUM_DISTANCE &&
                                b.getId2().toInt() == MAJOR && b.getId3().toInt() == MINOR) {
                            sendNotificationBeacon(b.getId1().toUuid().toString());
                        }
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
                .setContentTitle(getString(R.string.notif_warning_title))
                .setContentText(getString(R.string.notif_warning_description))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(uuid.hashCode(), builder.build());
    }


}
