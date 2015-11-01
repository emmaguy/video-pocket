package com.emmaguy.videopocket.feature.video;

import com.google.gson.annotations.SerializedName;

class PocketVideo {
    @SerializedName("item_id") private final long id;
    @SerializedName("resolved_title") private final String title;
    @SerializedName("resolved_url") private final String url;

    PocketVideo(long id, String title, String url) {
        this.id = id;
        this.title = title;
        this.url = url;
    }

    String getTitle() {
        return title;
    }

    String getUrl() {
        return url;
    }

    long getId() {
        return id;
    }
}
