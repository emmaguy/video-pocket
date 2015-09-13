package com.emmaguy.videopocket.video;

import java.util.Map;

import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.mime.TypedInput;
import rx.Observable;

public interface PocketApi {
    @POST("/v3/get")
    @Headers({"Content-Type: application/json", "X-Accept: application/json"}) Observable<Map<String, PocketVideo>> videos(@Body TypedInput body);
}
