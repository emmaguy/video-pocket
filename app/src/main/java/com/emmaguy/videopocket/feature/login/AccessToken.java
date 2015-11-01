package com.emmaguy.videopocket.feature.login;

import com.google.gson.annotations.SerializedName;

class AccessToken {
    @SerializedName("access_token") private String accessToken;
    @SerializedName("username") private String username;

    public String getAccessToken() {
        return accessToken;
    }

    public String getUsername() {
        return username;
    }
}
