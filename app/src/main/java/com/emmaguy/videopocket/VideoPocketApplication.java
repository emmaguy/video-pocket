package com.emmaguy.videopocket;

import com.emmaguy.videopocket.common.base.BaseApplication;
import com.jakewharton.threetenabp.AndroidThreeTen;

import timber.log.Timber;

public final class VideoPocketApplication extends BaseApplication {

    @Override public void onCreate() {
        super.onCreate();

        AndroidThreeTen.init(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
