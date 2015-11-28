package com.emmaguy.videopocket.feature.video;

import android.support.annotation.NonNull;

import org.threeten.bp.Duration;

class YouTubeVideo {
    private final String id;
    private final String viewCount;
    private final Duration duration;

    private static char[] formatSuffix = new char[]{'k', 'm', 'b', 't'};

    YouTubeVideo(@NonNull final String id, @NonNull final String duration, final long viewCount) {
        this.id = id;
        this.viewCount = format(viewCount, 0);
        this.duration = Duration.parse(duration);
    }

    @NonNull public String getId() {
        return id;
    }

    @NonNull public Duration getDuration() {
        return duration;
    }

    public String getViewCount() {
        return viewCount;
    }

    // Format a long number to the nearest unit - e.g. 1000 -> 1k
    private static String format(long n, int iteration) {
        if (n < 1000) {
            return String.valueOf(n);
        }

        long d = (n / 100) / 10;
        boolean isRound = (d * 10) % 10 == 0;
        return (d < 1000 ? ((d > 99.9 || isRound || (d > 9.99) ?
                (int) d * 10 / 10 : d + "") + "" + formatSuffix[iteration]) :
                format(d, iteration + 1));
    }
}
