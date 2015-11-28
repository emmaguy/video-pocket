package com.emmaguy.videopocket.feature.video;

import java.util.Map;

import retrofit.Result;
import retrofit.http.GET;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import rx.Observable;

interface YouTubeApi {
    @GET("/youtube/v3/videos?part=contentDetails,statistics") Observable<Result<YouTubeVideoResponse>> videoData(
            @QueryMap(encoded = false) final Map<String, String> videoIds, @Query("key") final String apiKey);
}
