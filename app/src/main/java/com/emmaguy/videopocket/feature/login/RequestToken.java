package com.emmaguy.videopocket.feature.login;

import com.google.gson.annotations.SerializedName;

class RequestToken {
    @SerializedName("code") private String code;

    public String getCode() {
        return code;
    }
}
