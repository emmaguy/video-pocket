package com.emmaguy.videopocket.feature.video;


import okhttp3.RequestBody;
import retrofit2.adapter.rxjava.Result;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

interface PocketApi {
    @POST("/v3/get") @Headers({"Content-Type: application/json", "X-Accept: application/json"})
    Observable<Result<PocketVideoResponse>> videos(@Body RequestBody body);

    @POST("/v3/send") @Headers({"Content-Type: application/x-www-form-urlencoded", "X-Accept: application/json"})
    Observable<Result<ActionResultResponse>> archive(@Query("consumer_key") String consumerKey, @Query("access_token") String accessToken, @Query("actions") String actions);
}
