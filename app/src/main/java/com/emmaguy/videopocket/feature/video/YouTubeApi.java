package com.emmaguy.videopocket.feature.video;

import java.util.Map;


import retrofit2.adapter.rxjava.Result;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import rx.Observable;

interface YouTubeApi {
    @GET("/youtube/v3/videos?part=contentDetails,statistics") Observable<Result<YouTubeVideoResponse>> videoData(
            @QueryMap(encoded = false) final Map<String, String> videoIds, @Query("key") final String apiKey);
}
