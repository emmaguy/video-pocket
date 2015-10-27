package com.emmaguy.videopocket.video;

import java.util.Map;

import retrofit.http.Body;
import retrofit.http.FormUrlEncoded;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Query;
import retrofit.mime.TypedInput;
import rx.Observable;

interface PocketApi {
    @POST("/v3/get")
    @Headers({"Content-Type: application/json", "X-Accept: application/json"})
    Observable<Map<String, PocketVideo>> videos(@Body TypedInput body);

    @POST("/v3/send")
    @Headers({"Content-Type: application/x-www-form-urlencoded", "X-Accept: application/json"})
    Observable<ActionResultResponse> archive(@Query("consumer_key") String consumerKey, @Query("access_token") String accessToken, @Query("actions") String actions, @Body String empty);
}
