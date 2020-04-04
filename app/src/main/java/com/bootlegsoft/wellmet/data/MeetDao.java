package com.bootlegsoft.wellmet.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MeetDao {
    @Query("SELECT * FROM meet")
    LiveData<List<Meet>> getAll();

    @Query("SELECT * FROM meet WHERE meetTime BETWEEN datetime('now', '-6 days') AND datetime('now', 'localtime');")
    LiveData<List<Meet>> loadAllFromLastWeek();

    @Insert
    void insertAll(Meet... meets);

    @Delete
    void delete(Meet meet);
}