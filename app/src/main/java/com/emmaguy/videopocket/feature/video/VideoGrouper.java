package com.emmaguy.videopocket.feature.video;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.Observable;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import timber.log.Timber;

class VideoGrouper {
    private static Func1<Collection<PocketVideo>, Map<String, Collection<PocketVideo>>> groupedVideos =
            videos -> toMap(Observable.from(videos)
                    .groupBy(VideoGrouper::getHost, v -> v));

    static Func1<Collection<PocketVideo>, Map<String, Collection<PocketVideo>>> groupBySource() {
        return groupedVideos;
    }

    private static <K, V> Map<K, Collection<V>> toMap(@NonNull final Observable<GroupedObservable<K, V>> observable) {
        final ConcurrentHashMap<K, Collection<V>> result = new ConcurrentHashMap<>();
        observable.forEach(o -> {
            result.put(o.getKey(), new ConcurrentLinkedQueue<>());
            o.subscribe(v -> result.get(o.getKey()).add(v));
        });

        return result;
    }

    @Nullable private static String getHost(@NonNull final PocketVideo v) {
        try {
            return new URL(v.getUrl()).getHost();
        } catch (MalformedURLException e) {
            Timber.e(e, "Failed to getHost from video url: " + v.getUrl());
        }
        return null;
    }
}