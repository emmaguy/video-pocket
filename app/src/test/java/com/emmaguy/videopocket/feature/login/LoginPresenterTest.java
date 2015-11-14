package com.emmaguy.videopocket.feature.login;

import com.emmaguy.videopocket.BasePresenterTest;
import com.emmaguy.videopocket.storage.UserStorage;
import com.google.gson.Gson;

import org.junit.Test;
import org.mockito.Mock;

import retrofit.Response;
import retrofit.Result;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LoginPresenterTest extends BasePresenterTest<LoginPresenter, LoginPresenter.View> {
    private static final String DEFAULT_REQUEST_TOKEN = "test_request_token";
    private static final String DEFAULT_ACCESS_TOKEN = "test_access_token";
    private static final String DEFAULT_CONSUMER_KEY = "test_consumer_key";
    private static final String DEFAULT_REDIRECT_URI = "test://callback";
    private static final String DEFAULT_USERNAME = "test_username";

    @Mock private PocketAuthenticationApi pocketAuthenticationApi;
    @Mock private UserStorage userStorage;
    private Gson gson = new Gson();

    private final PublishSubject<Void> mReturnFromBrowserSubject = PublishSubject.create();
    private final PublishSubject<Void> mLoginSubject = PublishSubject.create();

    @Override protected LoginPresenter createPresenter() {
        when(userStorage.getAccessToken()).thenReturn("");

        final RequestToken mockRequestToken = mock(RequestToken.class);
        when(mockRequestToken.getCode()).thenReturn(DEFAULT_REQUEST_TOKEN);

        when(pocketAuthenticationApi.requestToken(any()))
                .thenReturn(Observable.just(Result.response(Response.success(mockRequestToken))));

        final AccessToken mockAccessToken = mock(AccessToken.class);
        when(mockAccessToken.getUsername()).thenReturn(DEFAULT_USERNAME);
        when(mockAccessToken.getAccessToken()).thenReturn(DEFAULT_ACCESS_TOKEN);
        when(pocketAuthenticationApi.accessToken(any()))
                .thenReturn(Observable.just(Result.response(Response.success(mockAccessToken))));

        return new LoginPresenter(pocketAuthenticationApi, Schedulers.immediate(), Schedulers.immediate(),
                userStorage, DEFAULT_CONSUMER_KEY, DEFAULT_REDIRECT_URI, gson);
    }

    @Override protected LoginPresenter.View createView() {
        final LoginPresenter.View view = mock(LoginPresenter.View.class);
        when(view.retrieveRequestToken()).thenReturn(mLoginSubject);
        when(view.returnFromBrowser()).thenReturn(mReturnFromBrowserSubject);
        return view;
    }

    @Test
    public void onViewAttached_whenHasAccessToken_shouldStartVideos() throws Exception {
        when(userStorage.getAccessToken()).thenReturn(DEFAULT_ACCESS_TOKEN);

        presenterOnViewAttached();

        verify(view).startVideos();
        verifyNoMoreInteractions(view);

        verifyZeroInteractions(pocketAuthenticationApi);
    }

    @Test
    public void onViewAttached_whenHasNoAccessToken_subscribesToLoginActionAndReturnFromBrowserAction() throws Exception {
        presenterOnViewAttached();

        verify(view).retrieveRequestToken();
        verify(view).returnFromBrowser();
    }

    @Test
    public void onLoginAction_pocketApiThrowsException_showRequestTokenError() throws Exception {
        when(pocketAuthenticationApi.requestToken(any())).thenReturn(Observable.just(Result.error(new Throwable())));

        presenterOnViewAttached();
        onLoginAction();

        verify(view, never()).startBrowser(anyString());

        verify(view).hideLoadingView();
        verify(view).showRequestTokenError();
    }

    @Test
    public void afterErrorWithOnLoginAction_onLoginActionWithValidResponse_startsBrowser() throws Exception {
        final RequestToken mockRequestToken = mock(RequestToken.class);
        when(mockRequestToken.getCode()).thenReturn(DEFAULT_REQUEST_TOKEN);

        when(pocketAuthenticationApi.requestToken(any()))
                .thenReturn(Observable.just(Result.error(new Throwable())))
                .thenReturn(Observable.just(Result.response(Response.success(mockRequestToken))));

        presenterOnViewAttached();
        onLoginAction();

        onLoginAction();

        verify(view).startBrowser(anyString());
    }

    @Test
    public void onReturnFromBrowserAction_startVideos() throws Exception {
        presenterOnViewAttached();
        onReturnFromBrowserAction();

        verify(view).showLoadingView();

        verify(userStorage).storeUsername(DEFAULT_USERNAME);
        verify(userStorage).storeAccessToken(DEFAULT_ACCESS_TOKEN);
        verify(userStorage).storeRequestToken("");

        verify(view).hideLoadingView();

        verify(view).startVideos();
    }

    @Test
    public void onReturnFromBrowserAction_pocketApiThrowsException_showAccessTokenError() throws Exception {
        final RequestToken mockRequestToken = mock(RequestToken.class);
        when(mockRequestToken.getCode()).thenReturn(DEFAULT_REQUEST_TOKEN);

        final Response mockResponse = Response.error(500, null);
        final Result result = Result.response(mockResponse);

        when(pocketAuthenticationApi.accessToken(any())).thenReturn(Observable.just(result));

        presenterOnViewAttached();
        onReturnFromBrowserAction();

        verify(view, never()).startVideos();

        verify(view).hideLoadingView();
        verify(view).showAccessTokenError();
    }

    private void onLoginAction() {
        mLoginSubject.onNext(null);
    }

    private void onReturnFromBrowserAction() {
        mReturnFromBrowserSubject.onNext(null);
    }
}
