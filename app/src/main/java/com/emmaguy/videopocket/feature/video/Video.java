package com.emmaguy.videopocket.feature.video;

import android.support.annotation.NonNull;

import org.threeten.bp.Duration;

public class Video {
    private final long id;
    private final String title;
    private final String url;
    private final Duration duration;

    Video(final long id, @NonNull final String title, @NonNull final String url, @NonNull final Duration duration) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.duration = duration;
    }

    long getId() {
        return id;
    }

    @NonNull String getTitle() {
        return title;
    }

    @NonNull String getUrl() {
        return url;
    }

    @NonNull Duration getDuration() {
        return duration;
    }
}
