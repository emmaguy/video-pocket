package com.emmaguy.videopocket.storage;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.feature.video.SortOrder;

class SharedPreferencesUserStorage implements UserStorage {
    private final SharedPreferences sharedPreferences;
    private final Resources resources;

    SharedPreferencesUserStorage(@NonNull final SharedPreferences sharedPreferences, @NonNull final Resources resources) {
        this.sharedPreferences = sharedPreferences;
        this.resources = resources;
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
}
