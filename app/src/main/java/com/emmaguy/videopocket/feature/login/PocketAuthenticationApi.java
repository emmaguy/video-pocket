package com.emmaguy.videopocket.feature.login;

import com.squareup.okhttp.RequestBody;

import retrofit.Result;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;
import rx.Observable;

interface PocketAuthenticationApi {
    @POST("/v3/oauth/request")
    @Headers({"Content-Type: application/json", "X-Accept: application/json"})
    Observable<Result<RequestToken>> requestToken(@Body RequestBody body);

    @POST("/v3/oauth/authorize")
    @Headers({"Content-Type: application/json", "X-Accept: application/json"})
    Observable<Result<AccessToken>> accessToken(@Body RequestBody body);
}
