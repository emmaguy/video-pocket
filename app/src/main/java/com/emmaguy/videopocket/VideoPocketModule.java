package com.emmaguy.videopocket;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@Module final class VideoPocketModule {
    private final Context mContext;

    VideoPocketModule(@NonNull final Context context) {
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

    @Qualifier @Documented @Retention(RetentionPolicy.RUNTIME) public @interface ApplicationContext {
    }
}
