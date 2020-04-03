package com.bootlegsoft.wellmet;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;


import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;

import java.util.Collection;
import java.util.UUID;


public class IBeaconService extends Service implements BootstrapNotifier, BeaconConsumer {
    private static final String TAG = "IBeaconService";

    private static final String PREFIX = "com.bootlegsoft.wellmet.service.";
    public static final String ACTION_START = PREFIX + "ACTION_START";
    public static final String ACTION_STOP = PREFIX + "ACTION_STOP";

    public static final String BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    public static final String MAJOR = String.valueOf(0xC0);
    public static final String MINOR = String.valueOf(0x19);

    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "status";

    private BeaconManager beaconManager;

    private BackgroundPowerSaver backgroundPowerSaver;
    private BeaconTransmitter beaconTransmitter;

    public class ServiceControl extends Binder {
    }

    private final ServiceControl mBinder = new ServiceControl();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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
    }


    public static UUID getUUID() {
        return UUID.randomUUID(); // TODO Generate UUID from user phone number and hashing with date.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateNotification(); // Called as soon as possible to avoid ANR: startForegroundService/startForeground sequence
        String action = intent.getAction();
        switch (action) {
            case ACTION_START: {
                Log.i(TAG, "Action: starting beacon service");
                startAdvertise();
                startScan();
                break;
            }
            case ACTION_STOP: {
                Log.d(TAG, "Action: stopping beacon service");
                stopAdvertise();
                stopScan();
                break;
            }
            default: {
                Log.w(TAG, "Unknown action asked");
            }

        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }


    private void startScan() {
        Log.d(TAG, "Starting scan of beacons");
        beaconManager.bind(this);
    }

    public void stopScan() {
        beaconManager.unbind(this);
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
    public void didEnterRegion(Region region) {

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

                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("com.bootlegsoft.wellmet.rangingRegion", null, null, null));
        } catch (RemoteException e) {
        }
    }


    public static void start(Context context) {
        final Intent intent = new Intent(context, IBeaconService.class);
        intent.setAction(ACTION_START);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stop(Context context) {
        final Intent intent = new Intent(context, IBeaconService.class);
        intent.setAction(ACTION_STOP);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void bindService(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, IBeaconService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void updateNotification() {
        final Intent activityIntent = new Intent(IBeaconService.this, MainActivity.class);
        final PendingIntent activityPendingIntent = PendingIntent.getActivity(IBeaconService.this, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        final Intent stopBroadcastIntent = new Intent(IBeaconService.this, IBeaconService.class);
        stopBroadcastIntent.setAction(ACTION_STOP);
        final PendingIntent stopBroadcastPendingIntent = PendingIntent.getService(IBeaconService.this, 0, stopBroadcastIntent, PendingIntent.FLAG_CANCEL_CURRENT);
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
        final NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(IBeaconService.this, CHANNEL_ID);
        notifBuilder
                .setSmallIcon(R.drawable.ic_radio)
                //.setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notif_message))
                .addAction(R.drawable.ic_menu_pause, getString(R.string.notif_action_stop), stopBroadcastPendingIntent)
                .setContentIntent(activityPendingIntent);
        startForeground(NOTIFICATION_ID, notifBuilder.build());
    }


}
