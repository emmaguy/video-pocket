package com.emmaguy.videopocket.feature.video;

import android.net.Uri;
import android.support.annotation.NonNull;

class YouTubeParser {
    String getYouTubeId(@NonNull final String url) {
        return Uri.parse(url).getQueryParameter("v");
    }
}
