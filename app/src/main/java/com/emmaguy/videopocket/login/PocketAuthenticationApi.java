package com.emmaguy.videopocket.login;

import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.mime.TypedInput;
import rx.Observable;

public interface PocketAuthenticationApi {
    @POST("/v3/oauth/request")
    @Headers({"Content-Type: application/json", "X-Accept: application/json"}) Observable<RequestToken> requestToken(@Body TypedInput body);

    @POST("/v3/oauth/authorize")
    @Headers({"Content-Type: application/json", "X-Accept: application/json"}) Observable<AccessToken> accessToken(@Body TypedInput body);
}
