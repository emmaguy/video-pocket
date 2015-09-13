package com.emmaguy.videopocket.storage;

import android.content.SharedPreferences;
import android.content.res.Resources;

import com.emmaguy.videopocket.R;

class SharedPreferencesUserStorage implements UserStorage {
    private final SharedPreferences mSharedPreferences;
    private final Resources mResources;

    SharedPreferencesUserStorage(SharedPreferences sharedPreferences, Resources resources) {
        mSharedPreferences = sharedPreferences;
        mResources = resources;
    }

    @Override public void storeUsername(String username) {
        mSharedPreferences
                .edit()
                .putString(mResources.getString(R.string.pref_key_username), username)
                .apply();
    }

    @Override public void storeAccessToken(String accessToken) {
        mSharedPreferences
                .edit()
                .putString(mResources.getString(R.string.pref_key_access_token), accessToken)
                .apply();
    }

    @Override public void storeRequestToken(String requestTokenCode) {
        mSharedPreferences
                .edit()
                .putString(mResources.getString(R.string.pref_key_request_token), requestTokenCode)
                .apply();
    }

    @Override public String getUsername() {
        return mSharedPreferences.getString(mResources.getString(R.string.pref_key_username), "");
    }

    @Override public String getAccessToken() {
        return mSharedPreferences.getString(mResources.getString(R.string.pref_key_access_token), "");
    }

    @Override public String getRequestToken() {
        return mSharedPreferences.getString(mResources.getString(R.string.pref_key_request_token), "");
    }
}
