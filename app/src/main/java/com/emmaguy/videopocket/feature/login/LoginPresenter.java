package com.emmaguy.videopocket.feature.login;

import android.support.annotation.NonNull;

import com.emmaguy.videopocket.common.Results;
import com.emmaguy.videopocket.common.StringUtils;
import com.emmaguy.videopocket.common.base.BasePresenter;
import com.emmaguy.videopocket.common.base.PresenterView;
import com.emmaguy.videopocket.storage.UserStorage;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import rx.Observable;
import rx.Scheduler;
import rx.subjects.PublishSubject;
import timber.log.Timber;

class LoginPresenter extends BasePresenter<LoginPresenter.View> {
    private static final String BROWSER_REDIRECT_URL_REQUEST_TOKEN = "https://getpocket.com/auth/authorize?request_token=%s&redirect_uri=%s&mobile=1";
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

    private final PublishSubject<LoadingState> loadingStateSubject = PublishSubject.create();

    private final PocketAuthenticationApi pocketAuthenticationApi;

    private final Scheduler ioScheduler;
    private final Scheduler uiScheduler;

    private final UserStorage userStorage;

    private final String consumerKey;
    private final String callbackUrl;
    private final Gson gson;

    LoginPresenter(@NonNull final PocketAuthenticationApi pocketAuthenticationApi, @NonNull final Scheduler ioScheduler, @NonNull final Scheduler uiScheduler, @NonNull final UserStorage userStorage, @NonNull final String consumerKey, @NonNull final String callbackUrl, @NonNull final Gson gson) {
        this.pocketAuthenticationApi = pocketAuthenticationApi;
        this.ioScheduler = ioScheduler;
        this.uiScheduler = uiScheduler;
        this.consumerKey = consumerKey;
        this.userStorage = userStorage;
        this.callbackUrl = callbackUrl;
        this.gson = gson;
    }

    private enum LoadingState {
        IDLE,
        LOADING,
        ERROR_REQUEST_TOKEN,
        ERROR_ACCESS_TOKEN,
    }

    @Override public void onViewAttached(@NonNull final View view) {
        super.onViewAttached(view);

        if (!StringUtils.isEmpty(userStorage.getAccessToken())) {
            view.startVideos();
            return;
        }

        unsubscribeOnViewDetach(view.retrieveRequestToken()
                .observeOn(ioScheduler)
                .doOnNext(ignored -> loadingStateSubject.onNext(LoadingState.LOADING))
                .map(ignored -> gson.toJson(new RequestTokenRequestHolder(consumerKey, callbackUrl)))
                .map(json -> RequestBody.create(MEDIA_TYPE, json))
                .switchMap(pocketAuthenticationApi::requestToken)
                .doOnNext(requestTokenResult -> loadingStateSubject.onNext(LoadingState.IDLE))
                .doOnNext(requestTokenResult -> {
                    if (!Results.isSuccess(requestTokenResult)) {
                        loadingStateSubject.onNext(LoadingState.ERROR_REQUEST_TOKEN);
                    }
                })
                .filter(Results::isSuccess)
                .map(requestTokenResult -> requestTokenResult.response().body())
                .doOnNext(requestToken -> userStorage.storeRequestToken(requestToken.getCode()))
                .map(requestToken -> String.format(BROWSER_REDIRECT_URL_REQUEST_TOKEN, requestToken.getCode(), callbackUrl))
                .observeOn(uiScheduler)
                .subscribe(view::startBrowser, throwable -> Timber.e(throwable, "Failure retrieving request token")));

        unsubscribeOnViewDetach(view.returnFromBrowser()
                .observeOn(ioScheduler)
                .doOnNext(ignored -> loadingStateSubject.onNext(LoadingState.LOADING))
                .map(ignored -> gson.toJson(new AccessTokenRequestHolder(consumerKey, userStorage.getRequestToken())))
                .map(json -> RequestBody.create(MEDIA_TYPE, json))
                .switchMap(pocketAuthenticationApi::accessToken)
                .doOnNext(requestTokenResult -> loadingStateSubject.onNext(LoadingState.IDLE))
                .doOnNext(requestTokenResult -> {
                    if (!Results.isSuccess(requestTokenResult)) {
                        loadingStateSubject.onNext(LoadingState.ERROR_ACCESS_TOKEN);
                    }
                })
                .filter(Results::isSuccess)
                .map(requestTokenResult -> requestTokenResult.response().body())
                .doOnNext(accessToken -> {
                    userStorage.storeUsername(accessToken.getUsername());
                    userStorage.storeAccessToken(accessToken.getAccessToken());
                    userStorage.storeRequestToken("");
                })
                .observeOn(uiScheduler)
                .subscribe(accessToken -> view.startVideos(), throwable -> Timber.d(throwable, "Error returning from browser and getting access token in LoginPresenter")));

        unsubscribeOnViewDetach(loadingStateSubject.observeOn(uiScheduler).subscribe(loadingState -> {
            if (loadingState == LoadingState.LOADING) {
                view.showLoadingView();
            } else if (loadingState == LoadingState.IDLE) {
                view.hideLoadingView();
            } else if (loadingState == LoadingState.ERROR_REQUEST_TOKEN) {
                view.showRequestTokenError();
            } else if (loadingState == LoadingState.ERROR_ACCESS_TOKEN) {
                view.showAccessTokenError();
            }
        }));

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
