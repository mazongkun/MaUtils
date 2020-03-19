package com.mama.sample;

import android.app.Application;

import com.mama.sample.utils.FileUtils;
import com.mama.sample.utils.YuvUtils;

public class MaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        YuvUtils.init(this);
        FileUtils.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        YuvUtils.destroy();
    }
}
