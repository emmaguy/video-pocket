package com.emmaguy.videopocket.feature.video;

import com.google.gson.annotations.SerializedName;

class PocketVideo {
    @SerializedName("item_id") private final long mId;
    @SerializedName("resolved_title") private final String mTitle;
    @SerializedName("resolved_url") private final String mUrl;

    PocketVideo(long id, String title, String url) {
        mId = id;
        mTitle = title;
        mUrl = url;
    }

    String getTitle() {
        return mTitle;
    }

    String getUrl() {
        return mUrl;
    }

    long getId() {
        return mId;
    }
}
