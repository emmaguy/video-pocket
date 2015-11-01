package com.emmaguy.videopocket.feature.video;

import java.util.List;
import java.util.Map;

import retrofit.http.GET;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import rx.Observable;

interface YouTubeApi {
    @GET("/youtube/v3/videos?part=contentDetails") Observable<List<YouTubeVideo>> videoData(
            @QueryMap(encodeValues = false) final Map<String, String> videoIds, @Query("key") final String apiKey);
}
