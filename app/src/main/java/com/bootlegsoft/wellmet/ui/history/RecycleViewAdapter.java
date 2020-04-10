package com.bootlegsoft.wellmet.ui.history;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bootlegsoft.wellmet.R;
import com.bootlegsoft.wellmet.Utils;
import com.bootlegsoft.wellmet.data.Meet;

import java.text.DateFormat;
import java.util.List;

public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.RecyclerViewHolder> {

    private List<Meet> meetList;
    private View.OnClickListener clickListener;

    public RecycleViewAdapter(List<Meet> meetList, View.OnClickListener clickListener) {
        this.meetList = meetList;
        this.clickListener = clickListener;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerViewHolder holder, int position) {
        Meet meet = meetList.get(position);
        holder.distanceTextView.setText(String.format("%.2f m", meet.distance));
        holder.beaconIdTextView.setText(Utils.shortenUUID(meet.beaconId));
        holder.meetTimeTextView.setText(DateFormat.getDateTimeInstance().format(meet.meetTime));
        holder.itemView.setTag(meet);
        holder.itemView.setOnClickListener(clickListener);
        Log.d("meet", "Lon: " + meet.longitude + "Lat: " + meet.latitude);
        if (meet.latitude == 0 && meet.longitude == 0) {
            holder.locationImage.setVisibility(View.INVISIBLE);
        } else {
            holder.locationImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return meetList.size();
    }

    public void addItems(List<Meet> meetList) {
        this.meetList = meetList;
        notifyDataSetChanged();
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private TextView distanceTextView;
        private TextView beaconIdTextView;
        private TextView meetTimeTextView;
        private ImageView locationImage;

        RecyclerViewHolder(View view) {
            super(view);
            distanceTextView = (TextView) view.findViewById(R.id.distance);
            beaconIdTextView = (TextView) view.findViewById(R.id.beaconId);
            meetTimeTextView = (TextView) view.findViewById(R.id.meetTime);
            locationImage = (ImageView) view.findViewById(R.id.locationImage);
        }
    }
}