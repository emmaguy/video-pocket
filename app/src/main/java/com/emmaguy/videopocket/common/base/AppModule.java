package com.emmaguy.videopocket.common.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.emmaguy.videopocket.R;
import com.google.gson.Gson;

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

@Module final class AppModule {
    private final Context context;

    public AppModule(@NonNull final Context context) {
        this.context = context;
    }

    @Provides @Singleton @ApplicationContext Context provideContext() {
        return context.getApplicationContext();
    }

    @Provides @Singleton Resources provideResources() {
        return context.getResources();
    }

    @Provides @Singleton Gson provideGson() {
        return new Gson();
    }

    @Provides @Singleton SharedPreferences provideSharedPreferences(Resources resources) {
        return context.getSharedPreferences(resources.getString(R.string.shared_pref_name), Context.MODE_PRIVATE);
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
