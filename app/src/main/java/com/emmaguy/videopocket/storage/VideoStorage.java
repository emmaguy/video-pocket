package com.emmaguy.videopocket.storage;

import android.support.annotation.NonNull;

import com.emmaguy.videopocket.feature.video.Video;

import java.util.List;

public interface VideoStorage {
    @NonNull List<Video> getVideos();
    void storeVideos(@NonNull final List<Video> videos);
}
