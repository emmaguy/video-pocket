package com.emmaguy.videopocket.feature.video;

import android.support.annotation.NonNull;

import org.threeten.bp.Duration;

class YouTubeVideo {
    private final String id;
    private final Duration duration;

    YouTubeVideo(@NonNull final String id, @NonNull final String duration) {
        this.id = id;
        this.duration = Duration.parse(duration);
    }

    @NonNull public String getId() {
        return id;
    }

    @NonNull public Duration getDuration() {
        return duration;
    }
}
