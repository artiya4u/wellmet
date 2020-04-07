package com.bootlegsoft.wellmet.ui.history;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bootlegsoft.wellmet.data.AppDatabase;
import com.bootlegsoft.wellmet.data.Meet;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private AppDatabase appDatabase;
    private LiveData<List<Meet>> meets;

    public HistoryViewModel(Application application) {
        super(application);
        appDatabase = AppDatabase.getDatabase(this.getApplication());
        meets = appDatabase.meetDao().loadAllFromLastWeek();
    }

    public LiveData<List<Meet>> getMeets() {
        return meets;
    }
}