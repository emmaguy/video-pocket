package com.emmaguy.videopocket.video;

import android.net.Uri;

class YouTubeParser {
    String getYouTubeId(final String url) {
        return Uri.parse(url).getQueryParameter("v");
    }
}
