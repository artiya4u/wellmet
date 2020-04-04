package com.bootlegsoft.wellmet.ui.dashboard;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bootlegsoft.wellmet.data.AppDatabase;
import com.bootlegsoft.wellmet.data.AppExecutors;
import com.bootlegsoft.wellmet.data.Meet;
import com.bootlegsoft.wellmet.data.User;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private AppDatabase appDatabase;
    private LiveData<User> user;
    private LiveData<List<Meet>> meets;
    private LiveData<Integer> userCount;


    public DashboardViewModel(Application application) {
        super(application);
        appDatabase = AppDatabase.getDatabase(this.getApplication());

        user = appDatabase.userDao().getUser();
        meets = appDatabase.meetDao().loadAllFromLastWeek();
        userCount = appDatabase.meetDao().countUserFromLastWeak();
    }

    public LiveData<User> getUser() {
        return user;
    }

    public LiveData<List<Meet>> getMeets() {
        return meets;
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