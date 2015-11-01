package com.emmaguy.videopocket.feature.login;

import com.google.gson.annotations.SerializedName;

class AccessToken {
    @SerializedName("access_token") private String mAccessToken;
    @SerializedName("username") private String mUsername;

    public String getAccessToken() {
        return mAccessToken;
    }

    public String getUsername() {
        return mUsername;
    }
}
