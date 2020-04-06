package com.bootlegsoft.wellmet.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.bootlegsoft.wellmet.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Entity
public class User {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "phoneNumber")
    public String phoneNumber;

    @ColumnInfo(name = "enableAlert")
    public boolean enableAlert;

    @TypeConverters({TimestampConverter.class})
    @ColumnInfo(name = "createTime")
    public Date createTime;

    public byte[] wellKey() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(phoneNumber.getBytes());
            md.update(Utils.longToBytes(createTime.getTime()));
            return md.digest();
        } catch (NoSuchAlgorithmException cnse) {
            return null;
        }
    }
}
