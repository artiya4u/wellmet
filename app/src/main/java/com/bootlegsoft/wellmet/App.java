package com.bootlegsoft.wellmet;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bootlegsoft.wellmet.data.AppDatabase;
import com.bootlegsoft.wellmet.data.AppExecutors;
import com.bootlegsoft.wellmet.data.Meet;
import com.bootlegsoft.wellmet.data.User;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class App extends Application implements BeaconConsumer {
    private static final String TAG = "App";

    public static final String BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    // For identify COVID-19 beacons.
    public static final int MAJOR = 0xC0;
    public static final int MINOR = 0x19;

    public static final int IGNORE_PERIOD = 30 * 60 * 1000;

    public static final float ALERT_DISTANCE = 1.0f;
    public static final float MONITORING_DISTANCE = 2.0f;

    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "status";
    public static final String ALERT_CHANNEL_ID = "alert";
    public static final long MILLIS_PER_DAY = 86400 * 1000;

    private BeaconManager beaconManager;
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    BeaconParser beaconParser;
    private BeaconTransmitter beaconTransmitter;
    private LocationManager locationManager = null;
    private AlarmManager alarmManager;

    private AppDatabase appDatabase;
    private User user;
    private HashMap<String, Long> lastAlerts = new HashMap<>();

    private static App sInstance;

    public static App getInstance() {
        return sInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        appDatabase = AppDatabase.getDatabase(this);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // iBeacon parser
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_LAYOUT));
        beaconParser = new BeaconParser().setBeaconLayout(BEACON_LAYOUT);
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        updateNotification();
    }

    /**
     * @return the last know best location
     */
    private Location getLastBestLocation() {
        @SuppressLint("MissingPermission")
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        @SuppressLint("MissingPermission")
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) {
            GPSLocationTime = locationGPS.getTime();
        }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if (0 < GPSLocationTime - NetLocationTime) {
            return locationGPS;
        } else {
            return locationNet;
        }
    }


    private void getUser() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<User> users = appDatabase.userDao().getAll();
            if (users.size() == 0) {
                Log.d(TAG, "No user found!");
            } else {
                user = users.get(0);
                Log.d(TAG, "Loaded user: " + user.phoneNumber);
                startScan();
                startAdvertise();
            }
        });
    }


    public UUID getUUID() {
        long todayStart = (new Date().getTime() / MILLIS_PER_DAY) * MILLIS_PER_DAY;
        return Utils.genBeaconUUID(user.wellKey(), new Date(todayStart));
    }


    public void start() {
        getUser();
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

    public void startAdvertise() {
        Log.d(TAG, "Starting Advertising");
        if (beaconTransmitter == null) {
            beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        } else {
            beaconTransmitter.stopAdvertising();
        }

        UUID uuid = getUUID();
        Log.d(TAG, "UUID: " + uuid.toString());
        Beacon beacon = new Beacon.Builder()
                .setId1(uuid.toString())
                .setId2(String.valueOf(MAJOR))
                .setId3(String.valueOf(MINOR))
                .setManufacturer(0x004C)
                .setTxPower(-65)
                .build();

        beaconTransmitter.startAdvertising(beacon);
        Intent intent = new Intent(getApplicationContext(), RestartBeaconReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,  AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
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

                        String beaconId = b.getId1().toString();
                        Long lastAlert = lastAlerts.get(beaconId);
                        Date currentTime = new Date();

                        // Check last alert of a beacon.
                        if (lastAlert != null) {
                            // Prevent alert too often.
                            if (currentTime.getTime() - lastAlert < IGNORE_PERIOD) {
                                return;
                            }
                        }
                        if (b.getDistance() <= ALERT_DISTANCE && user.enableAlert) {
                            App.this.lastAlerts.put(beaconId, currentTime.getTime());
                            sendNotificationBeacon(beaconId);
                        }
                        if (b.getDistance() <= MONITORING_DISTANCE) {
                            Meet meet = new Meet();
                            meet.beaconId = beaconId;
                            meet.meetTime = currentTime;
                            meet.distance = b.getDistance();

                            Location lastBestLocation = getLastBestLocation();
                            meet.latitude = lastBestLocation.getLatitude();
                            meet.longitude = lastBestLocation.getLongitude();

                            AppExecutors.getInstance().diskIO().execute(() -> {
                                appDatabase.meetDao().insertAll(meet);
                            });
                        }
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("com.bootlegsoft.wellmet.rangingRegion", null, Identifier.fromInt(MAJOR), Identifier.fromInt(MINOR)));
        } catch (RemoteException e) {
        }
    }

    private void updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Alert Channel
            final CharSequence name = getString(R.string.notif_alert_channel_name);
            final String description = getString(R.string.notif_alert_channel_description);
            final int importance = NotificationManager.IMPORTANCE_HIGH;
            final NotificationChannel channel = new NotificationChannel(ALERT_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Status channel
            final CharSequence name = getString(R.string.notif_channel_name);
            final String description = getString(R.string.notif_channel_description);
            final int importance = NotificationManager.IMPORTANCE_LOW;
            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setVibrationPattern(new long[]{1000});
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_radio)
                .setContentTitle(getString(R.string.notif_warning_title))
                .setContentText(getString(R.string.notif_warning_description))
                .setVibrate(new long[]{1000})
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(uuid.hashCode(), builder.build());
    }

}
