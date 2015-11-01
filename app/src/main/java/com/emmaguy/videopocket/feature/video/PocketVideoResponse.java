package com.emmaguy.videopocket.feature.video;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

class PocketVideoResponse {
    @SerializedName("list") private final Map<String, PocketVideo> list;

    PocketVideoResponse(Map<String, PocketVideo> list) {
        this.list = list;
    }

    public Map<String, PocketVideo> getList() {
        return list;
    }
}
