package com.emmaguy.videopocket.video;

import android.support.annotation.NonNull;

import org.threeten.bp.Duration;

public class Video {
    private final long mId;
    private final String mTitle;
    private final String mUrl;
    private final Duration mDuration;

    Video(@NonNull final long id, @NonNull final String title, final String url, @NonNull final Duration duration) {
        mId = id;
        mTitle = title;
        mUrl = url;
        mDuration = duration;
    }

    long getId() {
        return mId;
    }

    @NonNull String getTitle() {
        return mTitle;
    }

    @NonNull String getUrl() {
        return mUrl;
    }

    @NonNull Duration getDuration() {
        return mDuration;
    }
}
