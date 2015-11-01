package com.emmaguy.videopocket.feature.video;

import org.threeten.bp.Duration;

class YouTubeVideo {
    private final String mId;
    private final Duration mDuration;

    YouTubeVideo(String id, String duration) {
        mId = id;
        mDuration = Duration.parse(duration);
    }

    public String getId() {
        return mId;
    }

    public Duration getDuration() {
        return mDuration;
    }
}
