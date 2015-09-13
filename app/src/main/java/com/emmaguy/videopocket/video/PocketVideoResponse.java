package com.emmaguy.videopocket.video;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

class PocketVideoResponse {
    @SerializedName("list") private final Map<String, PocketVideo> mList;

    PocketVideoResponse(Map<String, PocketVideo> list) {
        mList = list;
    }

    public Map<String, PocketVideo> getList() {
        return mList;
    }
}
