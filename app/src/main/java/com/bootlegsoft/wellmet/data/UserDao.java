package com.bootlegsoft.wellmet.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM user;")
    List<User> getAll();

    @Query("SELECT * FROM user LIMIT 1;")
    LiveData<User> getUser();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(User... users);

    @Update
    public void updateUsers(User... users);

    @Delete
    void delete(User user);
}