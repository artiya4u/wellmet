package com.bootlegsoft.wellmet.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

@Entity
public class User {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "phoneNumber")
    public String phoneNumber;

    @ColumnInfo(name = "tracingKey")
    public String tracingKey;

    @ColumnInfo(name = "enableAlert")
    public boolean enableAlert;

    @TypeConverters({TimestampConverter.class})
    @ColumnInfo(name = "createTime")
    public Date createTime;

}
