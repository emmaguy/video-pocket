package com.emmaguy.videopocket.feature.video;

import com.google.gson.annotations.SerializedName;

import java.util.List;

class YouTubeVideoResponse {
    @SerializedName("items") private final List<YouTubeResponse> items;

    YouTubeVideoResponse(List<YouTubeResponse> items) {
        this.items = items;
    }

    List<YouTubeResponse> getItems() {
        return items;
    }

    static class YouTubeResponse {
        @SerializedName("contentDetails") private final ContentDetails contentDetails;
        @SerializedName("id") private final String id;

        YouTubeResponse(ContentDetails contentDetails, String id) {
            this.contentDetails = contentDetails;
            this.id = id;
        }

        ContentDetails getContentDetails() {
            return contentDetails;
        }

        String getId() {
            return id;
        }

        static class ContentDetails {
            @SerializedName("duration") private final String duration;

            ContentDetails(String duration) {
                this.duration = duration;
            }

            String getDuration() {
                return duration;
            }
        }
    }
}
