package com.emmaguy.videopocket.common.base;

import com.emmaguy.videopocket.feature.ActivityComponent;
import com.emmaguy.videopocket.feature.ActivityModule;
import com.emmaguy.videopocket.storage.StorageModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton @Component(modules = {AppModule.class, StorageModule.class}) public interface AppComponent extends BaseComponent {
    ActivityComponent plus(ActivityModule module);
}
