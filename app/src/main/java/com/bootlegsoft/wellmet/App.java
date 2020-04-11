package com.bootlegsoft.wellmet;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
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

import java.util.ArrayList;
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

    public static final int IGNORE_ALERT_PERIOD = 30 * 60 * 1000;
    public static final int IGNORE_SAVE_PERIOD = 15 * 60 * 1000;
    public static final int RESTART_BEACON_INTERVAL = 15 * 60 * 1000;

    public static final float ALERT_DISTANCE = 1.0f;
    public static final float MONITORING_DISTANCE = 2.0f;

    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "status";
    public static final String ALERT_CHANNEL_ID = "alert";
    public static final long MILLIS_PER_DAY = 86400 * 1000;
    public static final long SCAN_INTERVAL = 10 * 1000;

    public static final int BEACON_MANUFACTURER = 0x004C;
    public static final int TX_POWER = -65;

    private BeaconManager beaconManager;
    private BackgroundPowerSaver backgroundPowerSaver;
    BeaconParser beaconParser;
    private BeaconTransmitter beaconTransmitter;
    private LocationManager locationManager = null;

    private AppDatabase appDatabase;
    private User user;
    private HashMap<String, Long> lastAlerts = new HashMap<>();
    private HashMap<String, Long> lastSeen = new HashMap<>();

    private static App sInstance;

    public static App getInstance() {
        return sInstance;
    }

    Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        appDatabase = AppDatabase.getDatabase(this);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // iBeacon parser
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_LAYOUT));
        beaconParser = new BeaconParser().setBeaconLayout(BEACON_LAYOUT);
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        createNotification();
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        handler = new Handler();
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

    public void setUser(User user) {
        this.user = user;
    }


    private void startWithUser() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            User user = appDatabase.userDao().getUser();
            if (user == null) {
                Log.d(TAG, "No user found!");
            } else {
                Log.d(TAG, "Loaded user: " + user.phoneNumber);
                startScan();
                startAdvertise();
            }
        });
    }


    public UUID getRollingProximityIdentifier() {
        long timeStampNow = new Date().getTime();
        long dayNumber = timeStampNow / MILLIS_PER_DAY;
        long todayStart = dayNumber * MILLIS_PER_DAY;
        // Smart phone is pretty fast, It's ok to hash a new one ever 15 minutes.
        byte[] dailyTracingKey = Utils.genDailyTracingKey(Utils.hexToBytes(user.tracingKey), dayNumber);
        // Guess timeIntervalNumber start with 0
        long timeIntervalNumber = (timeStampNow - todayStart) / RESTART_BEACON_INTERVAL;
        return Utils.genRollingProximityIdentifier(dailyTracingKey, timeIntervalNumber);
    }

    public List<String> getAllDailyTracingKey() {
        List<String> result = new ArrayList<>();
        long timeStampNow = new Date().getTime();
        long currentDate = user.createTime.getTime();
        while (currentDate <= timeStampNow) {
            long dayNumber = currentDate / MILLIS_PER_DAY;
            byte[] dailyTracingKey = Utils.genDailyTracingKey(Utils.hexToBytes(user.tracingKey), dayNumber);
            result.add(Utils.bytesToHex(dailyTracingKey));
            currentDate += MILLIS_PER_DAY;
        }
        return result;
    }


    public void start() {
        if (user == null) {
            startWithUser();
        }
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
    }

    public void startAdvertise() {
        Log.d(TAG, "Starting Advertising");
        if (beaconTransmitter == null) {
            beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        } else {
            beaconTransmitter.stopAdvertising();
        }

        UUID uuid = getRollingProximityIdentifier();
        Log.d(TAG, "Beacon UUID: " + uuid.toString());
        Beacon beacon = new Beacon.Builder()
                .setId1(uuid.toString())
                .setId2(String.valueOf(MAJOR))
                .setId3(String.valueOf(MINOR))
                .setManufacturer(BEACON_MANUFACTURER)
                .setTxPower(TX_POWER)
                .build();

        beaconTransmitter.startAdvertising(beacon);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startAdvertise();
            }
        }, RESTART_BEACON_INTERVAL);
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
                        Log.i(TAG, "Found Beacon ID: " + b.getId1() +
                                " Major: " + b.getId2() +
                                " Minor: " + b.getId3() +
                                " Distance: " + String.format("%.2f m", b.getDistance()));

                        String beaconId = b.getId1().toString();

                        Date currentTime = new Date();
                        Meet meet = new Meet();
                        meet.beaconId = beaconId;
                        meet.meetTime = currentTime;
                        meet.distance = b.getDistance();

                        // Check last alert of a beacon.
                        Long lastAlert = lastAlerts.get(beaconId);
                        if (lastAlert != null) {
                            // Prevent alert too often.
                            if (currentTime.getTime() - lastAlert < IGNORE_ALERT_PERIOD) {
                                return;
                            }
                        }
                        if (b.getDistance() <= ALERT_DISTANCE) {
                            App.this.lastAlerts.put(beaconId, currentTime.getTime());
                            App.this.lastSeen.put(beaconId, currentTime.getTime());
                            saveMeet(meet);
                            if (user.enableAlert) {
                                sendNotificationBeacon(beaconId);
                            }
                            return;
                        }

                        // Check last seen of a beacon.
                        Long seen = lastSeen.get(beaconId);
                        if (seen != null) {
                            // Prevent save data too often.
                            if (currentTime.getTime() - seen < IGNORE_SAVE_PERIOD) {
                                return;
                            }
                        }
                        if (b.getDistance() <= MONITORING_DISTANCE) {
                            App.this.lastSeen.put(beaconId, currentTime.getTime());
                            saveMeet(meet);
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

    private void saveMeet(Meet meet) {
        Location lastBestLocation = getLastBestLocation();
        if (lastBestLocation != null) {
            meet.latitude = lastBestLocation.getLatitude();
            meet.longitude = lastBestLocation.getLongitude();
        }
        Log.i(TAG, "Saving Beacon ID: " + meet.beaconId +
                " Time: " + meet.meetTime.toString() +
                " Latitude: " + meet.latitude +
                " Longitude: " + meet.longitude +
                " Distance: " + String.format("%.2f m", meet.distance));
        AppExecutors.getInstance().diskIO().execute(() -> {
            appDatabase.meetDao().insertAll(meet);
        });
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Alert Channel
            final CharSequence name = getString(R.string.notification_alert_channel_name);
            final String description = getString(R.string.notification_alert_channel_description);
            final int importance = NotificationManager.IMPORTANCE_HIGH;
            final NotificationChannel channel = new NotificationChannel(ALERT_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Status channel
            final CharSequence name = getString(R.string.notification_channel_name);
            final String description = getString(R.string.notification_channel_description);
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
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_message));

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
        beaconManager.setBackgroundScanPeriod(SCAN_INTERVAL);
    }

    public void sendNotificationBeacon(String uuid) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_radio)
                .setContentTitle(getString(R.string.notification_warning_title))
                .setContentText(getString(R.string.notification_warning_description))
                .setVibrate(new long[]{1000})
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(uuid.hashCode(), builder.build());
    }

}
