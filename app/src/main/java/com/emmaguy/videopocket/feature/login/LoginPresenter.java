package com.emmaguy.videopocket.feature.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.emmaguy.videopocket.common.Results;
import com.emmaguy.videopocket.common.StringUtils;
import com.emmaguy.videopocket.common.base.BasePresenter;
import com.emmaguy.videopocket.common.base.PresenterView;
import com.emmaguy.videopocket.storage.UserStorage;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.adapter.rxjava.Result;
import rx.Observable;
import rx.Scheduler;
import timber.log.Timber;

class LoginPresenter extends BasePresenter<LoginPresenter.View> {
    private static final String BROWSER_REDIRECT_URL_REQUEST_TOKEN = "https://getpocket.com/auth/authorize?request_token=%s&redirect_uri=%s&mobile=1";
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

    private final PocketAuthenticationApi pocketAuthenticationApi;

    private final Scheduler ioScheduler;
    private final Scheduler uiScheduler;

    private final UserStorage userStorage;

    private final String consumerKey;
    private final String callbackUrl;
    private final Gson gson;

    LoginPresenter(@NonNull final PocketAuthenticationApi pocketAuthenticationApi, @NonNull final Scheduler ioScheduler,
                   @NonNull final Scheduler uiScheduler, @NonNull final UserStorage userStorage,
                   @NonNull final String consumerKey, @NonNull final String callbackUrl, @NonNull final Gson gson) {
        this.pocketAuthenticationApi = pocketAuthenticationApi;
        this.ioScheduler = ioScheduler;
        this.uiScheduler = uiScheduler;
        this.consumerKey = consumerKey;
        this.userStorage = userStorage;
        this.callbackUrl = callbackUrl;
        this.gson = gson;
    }

    @Override public void onViewAttached(@NonNull final View view) {
        super.onViewAttached(view);

        if (!StringUtils.isEmpty(userStorage.getAccessToken())) {
            view.startVideos();
            return;
        }

        unsubscribeOnViewDetach(view.retrieveRequestToken()
                .doOnNext(v -> view.showLoadingView())
                .observeOn(ioScheduler)
                .flatMap(v -> {
                    final String json = gson.toJson(new RequestTokenRequestHolder(consumerKey, callbackUrl));
                    final RequestBody body = RequestBody.create(MEDIA_TYPE, json);
                    return pocketAuthenticationApi.requestToken(body);
                })
                .observeOn(uiScheduler)
                .map(result -> validRequestTokenOrNull(view, result))
                .observeOn(ioScheduler)
                .filter(requestToken -> requestToken != null)
                .doOnNext(requestToken -> userStorage.storeRequestToken(requestToken.getCode()))
                .map(requestToken -> String.format(BROWSER_REDIRECT_URL_REQUEST_TOKEN, requestToken.getCode(), callbackUrl))
                .observeOn(uiScheduler)
                .doOnNext(url -> view.hideLoadingView())
                .subscribe(view::startBrowser,
                        throwable -> Timber.d(throwable, "Error getting request token and launching browser in LoginPresenter")));

        unsubscribeOnViewDetach(view.returnFromBrowser()
                .doOnNext(v -> view.showLoadingView())
                .observeOn(ioScheduler)
                .flatMap(v -> {
                    final String json = gson.toJson(new AccessTokenRequestHolder(consumerKey, userStorage.getRequestToken()));
                    final RequestBody body = RequestBody.create(MEDIA_TYPE, json);
                    return pocketAuthenticationApi.accessToken(body);
                })
                .observeOn(uiScheduler)
                .map(result -> validAccessTokenOrNull(view, result))
                .filter(accessToken -> accessToken != null)
                .doOnNext(accessToken -> {
                    userStorage.storeUsername(accessToken.getUsername());
                    userStorage.storeAccessToken(accessToken.getAccessToken());
                    userStorage.storeRequestToken("");
                })
                .doOnNext(accessToken -> view.hideLoadingView())
                .subscribe(accessToken -> view.startVideos(),
                        throwable -> Timber.d(throwable, "Error returning from browser and getting access token in LoginPresenter")));
    }

    @Nullable
    private RequestToken validRequestTokenOrNull(@NonNull final View view, final Result<RequestToken> result) {
        if (!Results.isSuccess(result)) {
            view.hideLoadingView();
            view.showRequestTokenError();
            return null;
        }
        return result.response().body();
    }

    @Nullable private AccessToken validAccessTokenOrNull(@NonNull final View view, final Result<AccessToken> result) {
        if (!Results.isSuccess(result)) {
            view.hideLoadingView();
            view.showAccessTokenError();
            return null;
        }
        return result.response().body();
    }

    private static class AccessTokenRequestHolder {
        @SerializedName("consumer_key") final String consumerKey;
        @SerializedName("code") final String code;

        AccessTokenRequestHolder(final String consumerKey, final String code) {
            this.consumerKey = consumerKey;
            this.code = code;
        }
    }

    private static class RequestTokenRequestHolder {
        @SerializedName("consumer_key") final String consumerKey;
        @SerializedName("redirect_uri") final String redirectUri;

        RequestTokenRequestHolder(final String consumerKey, final String redirectUri) {
            this.consumerKey = consumerKey;
            this.redirectUri = redirectUri;
        }
    }

    public interface View extends PresenterView {
        @NonNull Observable<Void> retrieveRequestToken();
        @NonNull Observable<Void> returnFromBrowser();

        void showLoadingView();
        void hideLoadingView();

        void showRequestTokenError();
        void showAccessTokenError();

        void startBrowser(@NonNull final String url);
        void startVideos();
    }
}
