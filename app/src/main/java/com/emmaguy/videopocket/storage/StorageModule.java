package com.emmaguy.videopocket.storage;

import android.content.SharedPreferences;
import android.content.res.Resources;

import com.google.gson.Gson;

import dagger.Module;
import dagger.Provides;

@Module public final class StorageModule {
    @Provides UserStorage provideUserStorage(SharedPreferences sharedPreferences, Resources resources, Gson gson) {
        return new SharedPreferencesUserStorage(sharedPreferences, resources, gson);
    }

    @Provides VideoStorage provideVideoStorage(SharedPreferences sharedPreferences, Resources resources, Gson gson) {
        return new SharedPreferencesVideoStorage(sharedPreferences, resources, gson);
    }
}
