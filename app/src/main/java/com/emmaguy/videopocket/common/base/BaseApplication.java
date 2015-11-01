package com.emmaguy.videopocket.common.base;

import android.app.Application;
import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class BaseApplication extends Application {
    private final Map<String, ? super BaseComponent> components = new HashMap<>();

    private AppComponent component;

    @CallSuper @Override public void onCreate() {
        super.onCreate();

        component = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
    }

    public static BaseApplication with(@NonNull final Context context) {
        return ((BaseApplication) context.getApplicationContext());
    }

    public AppComponent getComponent() {
        return component;
    }

    public <C extends BaseComponent> C getComponent(@NonNull final String key) {
        //noinspection unchecked
        return (C) components.get(key);
    }

    public <C extends BaseComponent> void putComponent(@NonNull final String key, @NonNull C component) {
        components.put(key, component);
    }

    public void removeComponent(@NonNull final String key) {
        components.remove(key);
    }
}
