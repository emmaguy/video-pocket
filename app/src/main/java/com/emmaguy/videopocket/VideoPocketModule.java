package com.emmaguy.videopocket;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@Module public class VideoPocketModule {
    @NonNull private final Context mContext;

    public VideoPocketModule(@NonNull Context context) {
        mContext = context;
    }

    @Provides @Singleton @ApplicationContext Context provideContext() {
        return mContext.getApplicationContext();
    }

    @Provides @Singleton Resources provideResources() {
        return mContext.getResources();
    }

    @Provides @Singleton SharedPreferences provideSharedPreferences(Resources resources) {
        return mContext.getSharedPreferences(resources.getString(R.string.shared_pref_name), Context.MODE_PRIVATE);
    }

    @Provides @Singleton @Named("ui") public Scheduler provideUiScheduler() {
        return AndroidSchedulers.mainThread();
    }

    @Provides @Singleton @Named("io") public Scheduler provideIoScheduler() {
        return Schedulers.io();
    }
}
