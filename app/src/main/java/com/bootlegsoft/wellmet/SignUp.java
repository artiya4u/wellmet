package com.bootlegsoft.wellmet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.bootlegsoft.wellmet.data.AppDatabase;
import com.bootlegsoft.wellmet.data.AppExecutors;
import com.bootlegsoft.wellmet.data.User;
import com.bootlegsoft.wellmet.ui.AppViewModel;

import org.altbeacon.beacon.BeaconManager;

import java.util.Date;
import java.util.List;

public class SignUp extends AppCompatActivity {
    private static final String TAG = "SignUp";

    private AppDatabase appDatabase;
    private AppViewModel appViewModel;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        verifyBluetooth();

        final EditText phoneInput = findViewById(R.id.phone);
        final Button submitButton = findViewById(R.id.summit);

        appDatabase = AppDatabase.getDatabase(this);
        appViewModel = ViewModelProviders.of(this).get(AppViewModel.class);
        appViewModel.getUser().observe(this, updateUser -> {
            if (updateUser == null) {
                submitButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        final Editable phoneNumber = phoneInput.getText();
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            String phoneNumberOnly = phoneNumber.toString();
                            phoneNumberOnly = phoneNumberOnly.replace(" ", "");
                            phoneNumberOnly = phoneNumberOnly.replace("+", "");
                            phoneNumberOnly = phoneNumberOnly.replace("(", "");
                            phoneNumberOnly = phoneNumberOnly.replace(")", "");
                            User newUser = new User();
                            newUser.phoneNumber = phoneNumberOnly;
                            newUser.createTime = new Date();
                            newUser.enableAlert = true;
                            appDatabase.userDao().insertAll(newUser);
                            Log.d(TAG, "Create user: " + newUser.phoneNumber);
                        });
                        // Go to main activity.
                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                        startActivity(intent);
                    }
                });
            } else {
                Log.d(TAG, "Got User:" + updateUser.phoneNumber);
                // Go to main activity.
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        });
    }


    private void verifyBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.bluetooth_not_enable_title));
                builder.setMessage(getString(R.string.bluetooth_not_enable_msg));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.bluetooth_le_not_available_title));
            builder.setMessage(getString(R.string.bluetooth_le_not_available_msg));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }

            });
            builder.show();

        }

    }
}
