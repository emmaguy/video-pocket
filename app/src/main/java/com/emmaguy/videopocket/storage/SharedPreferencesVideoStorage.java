package com.emmaguy.videopocket.storage;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.feature.video.Video;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

class SharedPreferencesVideoStorage implements VideoStorage {
    private final SharedPreferences sharedPreferences;
    private final Resources resources;
    private final Gson gson;

    SharedPreferencesVideoStorage(@NonNull final SharedPreferences sharedPreferences, @NonNull final Resources resources, @NonNull final Gson gson) {
        this.sharedPreferences = sharedPreferences;
        this.resources = resources;
        this.gson = gson;
    }

    @Override @NonNull public List<Video> getVideos() {
        final String json = sharedPreferences.getString(resources.getString(R.string.pref_key_videos), "[]");
        return gson.fromJson(json, new TypeToken<List<Video>>() {}.getType());
    }

    @Override public void storeVideos(@NonNull final List<Video> videos) {
        sharedPreferences
                .edit()
                .putString(resources.getString(R.string.pref_key_videos), gson.toJson(videos))
                .apply();
    }
}
