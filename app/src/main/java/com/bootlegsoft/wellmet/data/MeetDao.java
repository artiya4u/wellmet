package com.bootlegsoft.wellmet.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MeetDao {
    @Query("SELECT * FROM meet")
    List<Meet> getAll();

    @Query("SELECT * FROM meet WHERE meetTime < date('now','-7 days') ORDER BY meetTime DESC;")
    LiveData<List<Meet>> loadAllFromLastWeek();

    @Query("SELECT COUNT(DISTINCT beaconId) FROM meet WHERE meetTime < date('now','-7 days');")
    LiveData<Integer> countUserFromLastWeak();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(Meet... meets);

    @Delete
    void delete(Meet meet);
}