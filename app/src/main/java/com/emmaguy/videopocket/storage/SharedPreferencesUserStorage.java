package com.emmaguy.videopocket.storage;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.emmaguy.videopocket.R;

class SharedPreferencesUserStorage implements UserStorage {
    private final SharedPreferences mSharedPreferences;
    private final Resources mResources;

    SharedPreferencesUserStorage(@NonNull final SharedPreferences sharedPreferences, @NonNull final Resources resources) {
        mSharedPreferences = sharedPreferences;
        mResources = resources;
    }

    @Override public void storeUsername(final String username) {
        mSharedPreferences
                .edit()
                .putString(mResources.getString(R.string.pref_key_username), username)
                .apply();
    }

    @Override public void storeAccessToken(final String accessToken) {
        mSharedPreferences
                .edit()
                .putString(mResources.getString(R.string.pref_key_access_token), accessToken)
                .apply();
    }

    @Override public void storeRequestToken(final String requestTokenCode) {
        mSharedPreferences
                .edit()
                .putString(mResources.getString(R.string.pref_key_request_token), requestTokenCode)
                .apply();
    }

    @Override @NonNull public String getUsername() {
        return mSharedPreferences.getString(mResources.getString(R.string.pref_key_username), "");
    }

    @Override @NonNull public String getAccessToken() {
        return mSharedPreferences.getString(mResources.getString(R.string.pref_key_access_token), "");
    }

    @Override @NonNull public String getRequestToken() {
        return mSharedPreferences.getString(mResources.getString(R.string.pref_key_request_token), "");
    }
}
