package com.emmaguy.videopocket;

import com.emmaguy.videopocket.common.base.BaseApplication;
import com.frogermcs.androiddevmetrics.AndroidDevMetrics;
import com.jakewharton.threetenabp.AndroidThreeTen;

import timber.log.Timber;

public final class VideoPocketApplication extends BaseApplication {

    @Override public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            AndroidDevMetrics.initWith(this);
        }

        AndroidThreeTen.init(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
