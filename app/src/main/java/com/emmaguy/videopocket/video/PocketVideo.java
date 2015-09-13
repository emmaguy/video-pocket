package com.emmaguy.videopocket.video;

import com.google.gson.annotations.SerializedName;

class PocketVideo {
    @SerializedName("resolved_title") private final String mTitle;
    @SerializedName("resolved_url") private final String mUrl;

    PocketVideo(String title, String url) {
        mTitle = title;
        mUrl = url;
    }

    String getTitle() {
        return mTitle;
    }

    String getUrl() {
        return mUrl;
    }
}
