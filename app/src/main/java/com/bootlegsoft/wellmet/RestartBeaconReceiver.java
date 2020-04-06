package com.bootlegsoft.wellmet;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class RestartBeaconReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Alarm Triggered", "RestartBeaconReceiver");
        Toast.makeText(context, "Alarm Triggered", Toast.LENGTH_SHORT).show();
        App.getInstance().startAdvertise();
    }

}
