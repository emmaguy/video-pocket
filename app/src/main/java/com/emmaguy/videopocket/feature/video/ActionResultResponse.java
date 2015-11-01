package com.emmaguy.videopocket.feature.video;

import com.google.gson.annotations.SerializedName;

class ActionResultResponse {
    @SerializedName("action_results") boolean[] actionResults;
    @SerializedName("status") int status;
}
