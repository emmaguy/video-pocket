package com.emmaguy.videopocket.feature.login;


import okhttp3.RequestBody;
import retrofit2.adapter.rxjava.Result;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import rx.Observable;

interface PocketAuthenticationApi {
    @POST("/v3/oauth/request") @Headers({"Content-Type: application/json", "X-Accept: application/json"})
    Observable<Result<RequestToken>> requestToken(@Body final RequestBody body);

    @POST("/v3/oauth/authorize") @Headers({"Content-Type: application/json", "X-Accept: application/json"})
    Observable<Result<AccessToken>> accessToken(@Body final RequestBody body);
}
