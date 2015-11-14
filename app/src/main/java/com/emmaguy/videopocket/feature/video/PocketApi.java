package com.emmaguy.videopocket.feature.video;

import com.squareup.okhttp.RequestBody;

import retrofit.Result;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Query;
import rx.Observable;

interface PocketApi {
    @POST("/v3/get")
    @Headers({"Content-Type: application/json", "X-Accept: application/json"})
    Observable<Result<PocketVideoResponse>> videos(@Body RequestBody body);

    @POST("/v3/send")
    @Headers({"Content-Type: application/x-www-form-urlencoded", "X-Accept: application/json"})
    Observable<Result<ActionResultResponse>> archive(@Query("consumer_key") String consumerKey,
                                                     @Query("access_token") String accessToken,
                                                     @Query("actions") String actions);
}
