package com.bootlegsoft.wellmet.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {User.class, Meet.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    public abstract MeetDao meetDao();
}
