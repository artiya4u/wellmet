package com.bootlegsoft.wellmet.ui.history;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bootlegsoft.wellmet.R;
import com.bootlegsoft.wellmet.data.Meet;

import java.util.ArrayList;

public class HistoryFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "HistoryFragment";
    private HistoryViewModel historyViewModel;
    private RecycleViewAdapter recyclerViewAdapter;
    private RecyclerView recyclerView;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        historyViewModel =
                ViewModelProviders.of(this).get(HistoryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerViewAdapter = new RecycleViewAdapter(new ArrayList<Meet>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(recyclerViewAdapter);

        historyViewModel.getMeets().observe(getViewLifecycleOwner(), meets -> {
            Log.d(TAG, "Meets:" + meets.size());
            recyclerViewAdapter.addItems(meets);
        });
        return root;
    }


    @Override
    public void onClick(View v) {
        Meet meet = (Meet) v.getTag();
        if (meet.longitude == 0 && meet.latitude == 0) { // No geo location recorded.
            return;
        }
        String uri = "http://maps.google.com/maps?q=loc:" + meet.latitude + "," + meet.longitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }
}
