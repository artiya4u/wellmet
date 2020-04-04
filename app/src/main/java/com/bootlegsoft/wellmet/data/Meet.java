package com.bootlegsoft.wellmet.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

@Entity
public class Meet {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "beaconId")
    public String beaconId;

    @TypeConverters({TimestampConverter.class})
    @ColumnInfo(name = "meetTime")
    public Date meetTime;

    @ColumnInfo(name = "distance")
    public double distance;

    @ColumnInfo(name = "longitude")
    public double longitude;

    @ColumnInfo(name = "latitude")
    public double latitude;
}
