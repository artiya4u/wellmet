package com.bootlegsoft.wellmet.ui.dashboard;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bootlegsoft.wellmet.data.AppDatabase;
import com.bootlegsoft.wellmet.data.AppExecutors;
import com.bootlegsoft.wellmet.data.User;


public class DashboardViewModel extends AndroidViewModel {

    private AppDatabase appDatabase;
    private LiveData<User> user;
    private LiveData<Integer> userCount;


    public DashboardViewModel(Application application) {
        super(application);
        appDatabase = AppDatabase.getDatabase(this.getApplication());

        user = appDatabase.userDao().getLiveUser();
        userCount = appDatabase.meetDao().countUserFromLastWeak();
    }

    public LiveData<User> getUser() {
        return user;
    }

    public LiveData<Integer> getUserCount() {
        return userCount;
    }

    public void updateUser(User user) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            appDatabase.userDao().updateUsers(user);
        });
    }
}