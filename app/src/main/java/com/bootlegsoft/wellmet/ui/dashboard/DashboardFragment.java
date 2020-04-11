package com.bootlegsoft.wellmet.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.bootlegsoft.wellmet.R;
import com.bootlegsoft.wellmet.data.User;

import java.text.DecimalFormat;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    private static final DecimalFormat formatter = new DecimalFormat("#,###,###");

    private DashboardViewModel dashboardViewModel;
    private User user;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        final TextView textViewLevel = root.findViewById(R.id.level);
        final TextView textViewMeetCount = root.findViewById(R.id.meetCount);
        final SwitchCompat alertSwitch = root.findViewById(R.id.alertSwitch);
        final TextView textViewAlertStatus = root.findViewById(R.id.alertStatus);
        final ImageView statusImage = root.findViewById(R.id.statusImage);

        statusImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialog = new LevelDescriptionDialogFragment();
                dialog.show(DashboardFragment.this.getParentFragmentManager(), "LevelDescriptionDialogFragment");
            }
        });

        alertSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (user != null) {
                    user.enableAlert = isChecked;
                    dashboardViewModel.updateUser(user);
                }
            }
        });

        dashboardViewModel.getUser().observe(getViewLifecycleOwner(), updateUser -> {
            if (updateUser != null) {
                Log.d(TAG, "Alert enable:" + updateUser.enableAlert);
                user = updateUser;
                alertSwitch.setChecked(updateUser.enableAlert);
                if (updateUser.enableAlert) {
                    textViewAlertStatus.setText(getString(R.string.alert_enabled));
                } else {
                    textViewAlertStatus.setText(getString(R.string.alert_disabled));
                }
            }
        });

        dashboardViewModel.getUserCount().observe(getViewLifecycleOwner(), count -> {
            Log.d(TAG, "Meets:" + count);
            Level level = getLevel(count);
            textViewMeetCount.setText(formatter.format(count));
            textViewLevel.setText(level.text);
            statusImage.setImageResource(level.image);
        });

        return root;
    }

    private static class Level {
        String text;
        int textColor;
        int image;
    }

    private Level getLevel(int userCount) {
        Level level = new Level();
        level.text = "-";

        if (userCount <= 200) {
            level.text = getString(R.string.level1);
            level.textColor = R.color.colorLevel1;
            level.image = R.drawable.ic_level1;
        } else if (userCount <= 400) {
            level.text = getString(R.string.level2);
            level.textColor = R.color.colorLevel2;
            level.image = R.drawable.ic_level2;
        } else if (userCount <= 800) {
            level.text = getString(R.string.level3);
            level.textColor = R.color.colorLevel3;
            level.image = R.drawable.ic_level3;
        } else {
            level.text = getString(R.string.level4);
            level.textColor = R.color.colorLevel4;
            level.image = R.drawable.ic_level4;
        }

        return level;
    }
}
