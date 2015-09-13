package com.emmaguy.videopocket.video;

import com.google.gson.annotations.SerializedName;

import java.util.List;

class YouTubeVideoResponse {
    @SerializedName("items") private final List<YouTubeResponse> mItems;

    YouTubeVideoResponse(List<YouTubeResponse> items) {
        mItems = items;
    }

    public List<YouTubeResponse> getItems() {
        return mItems;
    }

    static class YouTubeResponse {
        @SerializedName("contentDetails") private final ContentDetails mContentDetails;
        @SerializedName("id") private final String mId;

        YouTubeResponse(ContentDetails contentDetails, String id) {
            mContentDetails = contentDetails;
            mId = id;
        }

        public ContentDetails getContentDetails() {
            return mContentDetails;
        }

        public String getId() {
            return mId;
        }

        static class ContentDetails {
            @SerializedName("duration") private final String mDuration;

            ContentDetails(String duration) {
                mDuration = duration;
            }

            public String getDuration() {
                return mDuration;
            }
        }
    }
}
