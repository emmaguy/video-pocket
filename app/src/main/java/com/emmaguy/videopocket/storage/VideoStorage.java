package com.emmaguy.videopocket.storage;

import android.support.annotation.NonNull;

import com.emmaguy.videopocket.video.Video;

import java.util.List;

public interface VideoStorage {
    List<Video> getVideos();
    void storeVideos(@NonNull final List<Video> videos);
}
