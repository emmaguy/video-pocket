package com.emmaguy.videopocket.storage;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.common.StringUtils;
import com.emmaguy.videopocket.feature.video.SortOrder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SharedPreferencesUserStorage implements UserStorage {
    private final SharedPreferences sharedPreferences;
    private final Resources resources;
    private final Gson gson;

    SharedPreferencesUserStorage(@NonNull final SharedPreferences sharedPreferences, @NonNull final Resources resources, @NonNull final Gson gson) {
        this.sharedPreferences = sharedPreferences;
        this.resources = resources;
        this.gson = gson;
    }

    @Override @NonNull public String getUsername() {
        return sharedPreferences.getString(resources.getString(R.string.pref_key_username), "");
    }

    @Override public void storeUsername(final String username) {
        sharedPreferences
                .edit()
                .putString(resources.getString(R.string.pref_key_username), username)
                .apply();
    }

    @Override @NonNull public String getAccessToken() {
        return sharedPreferences.getString(resources.getString(R.string.pref_key_access_token), "");
    }

    @Override public void storeAccessToken(final String accessToken) {
        sharedPreferences
                .edit()
                .putString(resources.getString(R.string.pref_key_access_token), accessToken)
                .apply();
    }

    @Override @NonNull public String getRequestToken() {
        return sharedPreferences.getString(resources.getString(R.string.pref_key_request_token), "");
    }

    @Override public void storeRequestToken(final String requestTokenCode) {
        sharedPreferences
                .edit()
                .putString(resources.getString(R.string.pref_key_request_token), requestTokenCode)
                .apply();
    }

    @Override public SortOrder getSortOrder() {
        return SortOrder.fromIndex(sharedPreferences.getInt(resources.getString(R.string.pref_key_sort_order), SortOrder.VIDEO_DURATION.getIndex()));
    }

    @Override public void setSortOrder(final SortOrder sortOrder) {
        sharedPreferences
                .edit()
                .putInt(resources.getString(R.string.pref_key_sort_order), sortOrder.getIndex())
                .apply();
    }

    @NonNull @Override public Map<Integer, Collection<String>> getOtherSources() {
        final String json = sharedPreferences.getString(resources.getString(R.string.pref_key_other_sources), "");
        if (StringUtils.isEmpty(json)) {
            return new HashMap<>();
        }
        return gson.fromJson(json, new TypeToken<Map<Integer, Collection<String>>>() {}.getType());
    }

    @Override public void storeOtherSources(final Map<Integer, Collection<String>> otherSources) {
        sharedPreferences
                .edit()
                .putString(resources.getString(R.string.pref_key_other_sources), gson.toJson(otherSources))
                .apply();
    }
}
