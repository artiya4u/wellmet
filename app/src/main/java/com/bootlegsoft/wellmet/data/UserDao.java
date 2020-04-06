package com.bootlegsoft.wellmet.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {
    @Query("SELECT * FROM user LIMIT 1;")
    User getUser();

    @Query("SELECT * FROM user LIMIT 1;")
    LiveData<User> getLiveUser();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(User... users);

    @Update
    public void updateUsers(User... users);

    @Delete
    void delete(User user);
}