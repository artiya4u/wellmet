package com.bootlegsoft.wellmet;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import net.alea.beaconsimulator.bluetooth.ExtendedAdvertiseData;
import net.alea.beaconsimulator.bluetooth.model.IBeacon;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;


public class IBeaconSimulatorService extends Service {

    private static final Logger sLogger = LoggerFactory.getLogger(IBeaconSimulatorService.class);

    private static final String PREFIX = "com.bootlegsoft.wellmet.service.";
    public static final String ACTION_START = PREFIX + "ACTION_START";
    public static final String ACTION_STOP = PREFIX + "ACTION_STOP";

    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "status";

    private static IBeaconSimulatorService sInstance;

    private BluetoothAdapter mBtAdapter;
    private BluetoothLeAdvertiser mBtAdvertiser;

    private AdvertiseCallback mAdvertiseCallback;

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            sLogger.info("New BLE device found: {}", result.getDevice().getAddress());
            System.out.println(result.getDevice().getAddress());
        }

        public void onBatchScanResults(List<ScanResult> results) {
        }

        public void onScanFailed(int errorCode) {
            sLogger.error("BLE scan fail: {}", errorCode);
        }
    };
    private boolean mIsScanning = false;
    private BluetoothLeScanner mBleScanner;

    public class ServiceControl extends Binder {
    }

    private final ServiceControl mBinder = new ServiceControl();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    final int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (btState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            // TODO Notify Bluetooth off.
                            break;
                    }
                    break;
                }
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        mBtAdapter = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        registerReceiver(mBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        sInstance = this;
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
                sLogger.info("Action: starting new broadcast");
                startBroadcast(startId, false);
                startBeaconScan();
                break;
            }
            case ACTION_STOP: {
                sLogger.debug("Action: stopping a broadcast");
                stopBroadcast(startId, false);
                stopBeaconScan();
                break;
            }
            default: {
                sLogger.warn("Unknown action asked");
            }

        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        super.onDestroy();
        sLogger.debug("onDestroy() called");
        stopBroadcast(0, true);
        unregisterReceiver(mBroadcastReceiver);
        EventBus.getDefault().unregister(this);
    }


    private void startBeaconScan() {
        if (mIsScanning) {
            return;
        }
        sLogger.debug("Starting scan of beacons");
        mIsScanning = true;
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        mBleScanner = mBtAdapter.getBluetoothLeScanner();
        mBleScanner.startScan(null, builder.build(), mScanCallback);
    }

    public void stopBeaconScan() {
        if (!mIsScanning) {
            return;
        }
        sLogger.debug("Stopping scan of beacons");
        mIsScanning = false;
        if (mBtAdapter.getState() == BluetoothAdapter.STATE_ON) {
            mBleScanner.stopScan(mScanCallback);
        }
    }


    public AdvertiseSettings generateADSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setConnectable(false);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        return builder.build();
    }

    public ExtendedAdvertiseData generateADData() {
        IBeacon beacon = new IBeacon();
        beacon.setProximityUUID(getUUID());
        beacon.setMajor(0xC0);
        beacon.setMinor(0x19);
        return new ExtendedAdvertiseData(beacon.generateAdvertiseData());
    }

    private void startBroadcast(int serviceStartId, boolean isRestart) {
        if (!isRestart && mAdvertiseCallback != null) {
            sLogger.info("Already broadcasting this beacon model, skipping");
            return;
        }
        mBtAdvertiser = mBtAdapter.getBluetoothLeAdvertiser();
        if (mBtAdvertiser == null || !mBtAdapter.isEnabled()) {
            sLogger.warn("Bluetooth is off, doing nothing");
            return;
        }
        final AdvertiseSettings settings = generateADSettings();
        final ExtendedAdvertiseData exAdvertiseData = generateADData();
        final AdvertiseData advertiseData = exAdvertiseData.getAdvertiseData();
        mAdvertiseCallback = new MyAdvertiseCallback(serviceStartId);
        mBtAdvertiser.startAdvertising(settings, advertiseData, mAdvertiseCallback);
    }


    private void stopBroadcast(int serviceStartId, boolean isRestart) {
        try {
            if (mBtAdvertiser != null) {
                mBtAdvertiser.stopAdvertising(mAdvertiseCallback);
            } else {
                sLogger.warn("Not able to stop broadcast; mBtAdvertiser is null");
            }
        } catch (RuntimeException e) { // Can happen if BT adapter is not in ON state
            sLogger.warn("Not able to stop broadcast; BT state: {}", mBtAdapter.isEnabled(), e);
        }
        if (!isRestart) {
            updateNotification();
        }
    }


    public static void startBroadcast(Context context) {
        final Intent intent = new Intent(context, IBeaconSimulatorService.class);
        intent.setAction(ACTION_START);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stopBroadcast(Context context) {
        final Intent intent = new Intent(context, IBeaconSimulatorService.class);
        intent.setAction(ACTION_STOP);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void bindService(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, IBeaconSimulatorService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public static void unbindService(Context context, ServiceConnection serviceConnection) {
        context.unbindService(serviceConnection);
    }

    public static boolean isBluetoothOn(Context context) {
        final BluetoothAdapter bluetoothAdapter = ((BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        return (bluetoothAdapter != null && bluetoothAdapter.isEnabled());
    }

    public static boolean isBroadcastAvailable(Context context) {
        final BluetoothAdapter bluetoothAdapter = ((BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isMultipleAdvertisementSupported();
    }


    private class MyAdvertiseCallback extends AdvertiseCallback {
        int serviceStartId;

        MyAdvertiseCallback(int serviceStartId) {
            this.serviceStartId = serviceStartId;
        }

        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            updateNotification();
            sLogger.info("Success in starting broadcast");
        }

        public void onStartFailure(int errorCode) {
            int reason;
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    reason = R.string.advertise_error_already_started;
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    reason = R.string.advertise_error_data_large;
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    reason = R.string.advertise_error_unsupported;
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    reason = R.string.advertise_error_internal;
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    reason = R.string.advertise_error_too_many;
                    break;
                default:
                    reason = R.string.advertise_error_unknown;
            }
            Toast.makeText(IBeaconSimulatorService.this, reason, Toast.LENGTH_SHORT).show();
            sLogger.warn("Error starting broadcasting: {}", reason);
        }
    }

    private void updateNotification() {
        final Intent activityIntent = new Intent(IBeaconSimulatorService.this, MainActivity.class);
        final PendingIntent activityPendingIntent = PendingIntent.getActivity(IBeaconSimulatorService.this, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        final Intent stopBroadcastIntent = new Intent(IBeaconSimulatorService.this, IBeaconSimulatorService.class);
        stopBroadcastIntent.setAction(ACTION_STOP);
        final PendingIntent stopBroadcastPendingIntent = PendingIntent.getService(IBeaconSimulatorService.this, 0, stopBroadcastIntent, PendingIntent.FLAG_CANCEL_CURRENT);
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
        final NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(IBeaconSimulatorService.this, CHANNEL_ID);
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
