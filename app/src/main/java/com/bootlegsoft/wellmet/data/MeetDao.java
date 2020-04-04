package com.bootlegsoft.wellmet.data;

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

    @Query("SELECT * FROM meet WHERE meetTime BETWEEN datetime('now', '-6 days') AND datetime('now', 'localtime') ORDER BY meetTime ASC;")
    List<Meet> loadAllFromLastWeek();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(Meet... meets);

    @Delete
    void delete(Meet meet);
}