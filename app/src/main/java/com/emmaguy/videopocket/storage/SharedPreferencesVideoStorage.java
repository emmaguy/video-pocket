package com.emmaguy.videopocket.storage;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.video.Video;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

class SharedPreferencesVideoStorage implements VideoStorage {
    private final SharedPreferences mSharedPreferences;
    private final Resources mResources;
    private final Gson mGson;

    SharedPreferencesVideoStorage(@NonNull final SharedPreferences sharedPreferences, @NonNull final Resources resources, @NonNull final Gson gson) {
        mSharedPreferences = sharedPreferences;
        mResources = resources;
        mGson = gson;
    }

    @Override @NonNull public List<Video> getVideos() {
        final String json = mSharedPreferences.getString(mResources.getString(R.string.pref_key_videos), "[]");
        return mGson.fromJson(json, new TypeToken<List<Video>>() {
        }.getType());
    }

    @Override public void storeVideos(@NonNull final List<Video> videos) {
        mSharedPreferences
                .edit()
                .putString(mResources.getString(R.string.pref_key_videos), mGson.toJson(videos))
                .apply();
    }
}
