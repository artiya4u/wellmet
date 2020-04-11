package com.bootlegsoft.wellmet.ui.profile;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.bootlegsoft.wellmet.App;
import com.bootlegsoft.wellmet.BuildConfig;
import com.bootlegsoft.wellmet.R;
import com.bootlegsoft.wellmet.data.User;
import com.bootlegsoft.wellmet.ui.AppViewModel;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private static final String APP_VERSION = "v." + BuildConfig.VERSION_NAME;

    private AppViewModel appViewModel;
    private User user;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        appViewModel =
                ViewModelProviders.of(this).get(AppViewModel.class);
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        TextView userCodeView = root.findViewById(R.id.user_code);
        userCodeView.setOnClickListener(v -> {
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("USER_CODE", user.tracingKey);
            clipboard.setPrimaryClip(clip);
            Context context = getActivity();
            CharSequence text = getString(R.string.copied);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        });

        TextView notificationSettingView = root.findViewById(R.id.notification_setting);
        notificationSettingView.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", this.getActivity().getPackageName());
            intent.putExtra("app_uid", this.getActivity().getApplicationInfo().uid);
            intent.putExtra("android.provider.extra.APP_PACKAGE", this.getActivity().getPackageName());
            startActivity(intent);
        });
        TextView howItWorkView = root.findViewById(R.id.how_it_work);
        howItWorkView.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.how_it_work_link)));
            startActivity(browserIntent);
        });

        TextView tellYourFriendView = root.findViewById(R.id.tell_your_friends);
        tellYourFriendView.setOnClickListener(v -> {
            String shareBody = getString(R.string.share_text);
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_title));
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.share)));
        });

        TextView appVersionView = root.findViewById(R.id.app_version);
        appVersionView.setText(APP_VERSION);

        appViewModel.getUser().observe(getViewLifecycleOwner(), updateUser -> {
            if (updateUser != null) {
                user = updateUser;
                String stars = "****************************";
                String showCode = user.tracingKey.substring(0, 4) + stars;
                userCodeView.setText(showCode);
            }
        });
        String issuesURL = "https://github.com/artiya4u/wellmet/issues";
        TextView rateView = root.findViewById(R.id.rate);
        rateView.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(issuesURL));
            startActivity(browserIntent);
        });

        TextView exportYourDataView = root.findViewById(R.id.export_your_data);
        exportYourDataView.setOnClickListener(v -> {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            String formattedDate = fmt.format(new Date());
            DiagnosisKey export = new DiagnosisKey();
            export.dailyTracingKeys = App.getInstance().getAllDailyTracingKey();

            Gson gson = new Gson();
            String exportedJson = gson.toJson(export);
            Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity())
                    .setType("application/json")
                    .setText(exportedJson)
                    .setSubject("WellMet-" + formattedDate + ".json")
                    .getIntent();
            startActivity(shareIntent);
        });
        return root;
    }

    private static class DiagnosisKey {
        List<String> dailyTracingKeys = new ArrayList<>();
    }
}
