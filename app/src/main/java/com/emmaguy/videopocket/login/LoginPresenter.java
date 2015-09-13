package com.emmaguy.videopocket.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.emmaguy.videopocket.Presenter;
import com.emmaguy.videopocket.PresenterView;
import com.emmaguy.videopocket.StringUtils;
import com.emmaguy.videopocket.storage.UserStorage;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.UnsupportedEncodingException;

import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import rx.Observable;
import rx.Scheduler;
import timber.log.Timber;

class LoginPresenter extends Presenter<LoginPresenter.View> {
    private static final String BROWSER_REDIRECT_URL_REQUEST_TOKEN = "https://getpocket.com/auth/authorize?request_token=%s&redirect_uri=%s&mobile=1";

    private final PocketAuthenticationApi mPocketApi;

    private final Scheduler mIoScheduler;
    private final Scheduler mUiScheduler;

    private final UserStorage mUserStorage;

    private final String mConsumerKey;
    private final String mCallbackUrl;

    LoginPresenter(@NonNull final PocketAuthenticationApi pocketApi, @NonNull final Scheduler ioScheduler, @NonNull final Scheduler uiScheduler,
                   @NonNull final UserStorage userStorage, @NonNull final String consumerKey, @NonNull final String callbackUrl) {
        mPocketApi = pocketApi;
        mIoScheduler = ioScheduler;
        mUiScheduler = uiScheduler;
        mConsumerKey = consumerKey;
        mUserStorage = userStorage;
        mCallbackUrl = callbackUrl;
    }

    @Override public void onViewAttached(@NonNull final View view) {
        super.onViewAttached(view);

        if (!StringUtils.isEmpty(mUserStorage.getAccessToken())) {
            view.startVideos();
            return;
        }

        unsubscribeOnViewDetach(view.retrieveRequestTokenObservable()
                .doOnNext(v -> view.showLoadingView())
                .observeOn(mIoScheduler)
                .flatMap(v -> Observable.defer(() -> mPocketApi.requestToken(buildJson(new RequestTokenRequestHolder(mConsumerKey, mCallbackUrl)))).onErrorResumeNext(Observable.just(null)))
                .observeOn(mUiScheduler)
                .map(requestToken -> validRequestTokenOrNull(view, requestToken))
                .filter(requestToken -> requestToken != null)
                .doOnNext(requestToken -> mUserStorage.storeRequestToken(requestToken.getCode()))
                .map(requestToken -> String.format(BROWSER_REDIRECT_URL_REQUEST_TOKEN, requestToken.getCode(), mCallbackUrl))
                .doOnNext(url -> view.hideLoadingView())
                .subscribe(view::startBrowser, throwable -> Timber.d(throwable, "Fatal error getting request token and launching browser in LoginPresenter")));

        unsubscribeOnViewDetach(view.returnFromBrowserObservable()
                .doOnNext(v -> view.showLoadingView())
                .observeOn(mIoScheduler)
                .flatMap(v -> Observable.defer(() -> mPocketApi.accessToken(buildJson(new AccessTokenRequestHolder(mConsumerKey, mUserStorage.getRequestToken())))).onErrorResumeNext(Observable.just(null)))
                .observeOn(mUiScheduler)
                .map(accessToken -> validAccessTokenOrNull(view, accessToken))
                .filter(accessToken -> accessToken != null)
                .doOnNext(accessToken -> {
                    mUserStorage.storeUsername(accessToken.getUsername());
                    mUserStorage.storeAccessToken(accessToken.getAccessToken());
                    mUserStorage.storeRequestToken("");
                })
                .doOnNext(accessToken -> view.hideLoadingView())
                .subscribe(accessToken -> view.startVideos(), throwable -> Timber.d(throwable, "Failure returning from browser and getting access token in LoginPresenter")));
    }

    @Nullable
    private RequestToken validRequestTokenOrNull(@NonNull final View view, final RequestToken requestToken) {
        if (requestToken == null || StringUtils.isEmpty(requestToken.getCode())) {
            view.hideLoadingView();
            view.showRequestTokenError();
            return null;
        }
        return requestToken;
    }

    @Nullable
    private AccessToken validAccessTokenOrNull(@NonNull final View view, final AccessToken accessToken) {
        if (accessToken == null || StringUtils.isEmpty(accessToken.getAccessToken())) {
            view.hideLoadingView();
            view.showAccessTokenError();
            return null;
        }
        return accessToken;
    }

    @NonNull private TypedInput buildJson(final Object o) {
        final String json = new Gson().toJson(o);
        try {
            return new TypedByteArray("application/json", json.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Failed to create json");
            throw new RuntimeException("Failed to build json", e);
        }
    }

    private static class AccessTokenRequestHolder {
        @SerializedName("consumer_key") final String mConsumerKey;
        @SerializedName("code") final String mCode;

        AccessTokenRequestHolder(final String consumerKey, final String code) {
            mConsumerKey = consumerKey;
            mCode = code;
        }
    }

    private static class RequestTokenRequestHolder {
        @SerializedName("consumer_key") final String mConsumerKey;
        @SerializedName("redirect_uri") final String mRedirectUri;

        RequestTokenRequestHolder(final String consumerKey, final String redirectUri) {
            mConsumerKey = consumerKey;
            mRedirectUri = redirectUri;
        }
    }

    public interface View extends PresenterView {
        @NonNull Observable<Void> retrieveRequestTokenObservable();
        @NonNull Observable<Void> returnFromBrowserObservable();

        void showLoadingView();
        void hideLoadingView();

        void showRequestTokenError();
        void showAccessTokenError();

        void startBrowser(final @NonNull String url);
        void startVideos();
    }
}
