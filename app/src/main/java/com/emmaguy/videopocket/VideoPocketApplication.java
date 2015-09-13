package com.emmaguy.videopocket;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class VideoPocketApplication extends Application {
    private final Map<String, ? super BaseComponent> mComponents = new HashMap<>();
    private VideoPocketComponent mComponent;

    @Override public void onCreate() {
        super.onCreate();

        mComponent = DaggerVideoPocketComponent.builder().videoPocketModule(new VideoPocketModule(this)).build();

        AndroidThreeTen.init(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public static VideoPocketApplication with(@NonNull final Context context) {
        return ((VideoPocketApplication) context.getApplicationContext());
    }

    public VideoPocketComponent getComponent() {
        return mComponent;
    }

    public <C extends BaseComponent> C getComponent(@NonNull final String key) {
        //noinspection unchecked
        return (C) mComponents.get(key);
    }

    public <C extends BaseComponent> void putComponent(@NonNull final String key, @NonNull C component) {
        mComponents.put(key, component);
    }

    public void removeComponent(@NonNull final String key) {
        mComponents.remove(key);
    }
}
