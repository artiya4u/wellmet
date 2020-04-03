package com.bootlegsoft.wellmet;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class RebootReceiver extends BroadcastReceiver {
    private static final String TAG = ".RebootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                Log.i(TAG, "Android boot completed, restarting beacon broadcast...");
                final BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
                final boolean btResult = mBluetoothAdapter.enable();
                if (btResult) {
                    Log.i(TAG, "Enabling Bluetooth interface for beacon broadcast");
                } else {
                    Log.w(TAG, "Could not enable Bluetooth interface at reboot for beacon broadcast");
                }
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED: {
                final int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                Log.d(TAG, "Bluetooth state changed: " + btState);
                switch (btState) {
                    case BluetoothAdapter.STATE_ON:
                        Log.i(TAG, "Bluetooth is ON, restarting beacon broadcast");
                        App.getInstance().start();
                        break;
                }
                break;
            }
        }
    }

}
