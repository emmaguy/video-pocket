package com.emmaguy.videopocket.video;

import com.google.gson.annotations.SerializedName;

class ActionResultResponse {
    @SerializedName("action_results") boolean[] mActionResults;
    @SerializedName("status") int mStatus;
}
