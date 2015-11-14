package com.emmaguy.videopocket.feature.video;

import android.support.annotation.NonNull;

import com.emmaguy.videopocket.storage.UserStorage;

import rx.functions.Func2;

final class VideoSorter {
    static Func2<Video, Video, Integer> sort(@NonNull final UserStorage userStorage) {
        return (video, video2) -> {
            final boolean sortedByDuration = userStorage.getSortOrder() == SortOrder.VIDEO_DURATION;
            return sortedByDuration ? sortVideosByDuration(video, video2) : sortVideosById(video, video2);
        };
    }

    private static int sortVideosByDuration(@NonNull final Video video, @NonNull final Video video2) {
        if (video.getDuration().getSeconds() > video2.getDuration().getSeconds()) {
            return -1;
        } else if (video.getDuration().getSeconds() < video2.getDuration().getSeconds()) {
            return 1;
        }
        return 0;
    }

    private static int sortVideosById(@NonNull final Video video, @NonNull final Video video2) {
        if (video.getId() > video2.getId()) {
            return -1;
        } else if (video.getId() < video2.getId()) {
            return 1;
        }
        return 0;
    }
}
