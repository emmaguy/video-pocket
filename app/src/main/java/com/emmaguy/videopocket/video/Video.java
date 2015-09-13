package com.emmaguy.videopocket.video;

import android.support.annotation.NonNull;

import org.threeten.bp.Duration;

public class Video {
    private final String mTitle;
    private final String mUrl;
    private final Duration mDuration;

    Video(final String title, final String url, @NonNull final Duration duration) {
        mTitle = title;
        mUrl = url;
        mDuration = duration;
    }

    @NonNull Duration getDuration() {
        return mDuration;
    }

    String getUrl() {
        return mUrl;
    }

    String getTitle() {
        return mTitle;
    }
}
