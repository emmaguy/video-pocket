package com.emmaguy.videopocket;

import com.emmaguy.videopocket.storage.StorageModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton @Component(modules = {VideoPocketModule.class, StorageModule.class}) public interface VideoPocketComponent extends BaseComponent {
    ActivityComponent plus(ActivityModule module);
}
