package com.bootlegsoft.wellmet.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bootlegsoft.wellmet.data.AppDatabase;
import com.bootlegsoft.wellmet.data.User;

public class AppViewModel extends AndroidViewModel {
    private AppDatabase appDatabase;
    private LiveData<User> user;


    public AppViewModel(Application application) {
        super(application);
        appDatabase = AppDatabase.getDatabase(this.getApplication());

        user = appDatabase.userDao().getLiveUser();
    }

    public LiveData<User> getUser() {
        return user;
    }
}
