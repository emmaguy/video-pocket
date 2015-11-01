package com.emmaguy.videopocket.feature.login;

import com.emmaguy.videopocket.BasePresenterTest;
import com.emmaguy.videopocket.TestUtils;
import com.emmaguy.videopocket.storage.UserStorage;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;

import retrofit.RetrofitError;
import retrofit.mime.TypedInput;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import static junit.framework.Assert.assertEquals;
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

    private final PublishSubject<Void> mReturnFromBrowserSubject = PublishSubject.create();
    private final PublishSubject<Void> mLoginSubject = PublishSubject.create();

    @Override protected LoginPresenter createPresenter() {
        when(userStorage.getAccessToken()).thenReturn("");

        final RequestToken mockRequestToken = mock(RequestToken.class);
        when(mockRequestToken.getCode()).thenReturn(DEFAULT_REQUEST_TOKEN);

        when(pocketAuthenticationApi.requestToken(any())).thenReturn(Observable.just(mockRequestToken));

        final AccessToken mockAccessToken = mock(AccessToken.class);
        when(mockAccessToken.getUsername()).thenReturn(DEFAULT_USERNAME);
        when(mockAccessToken.getAccessToken()).thenReturn(DEFAULT_ACCESS_TOKEN);
        when(pocketAuthenticationApi.accessToken(any())).thenReturn(Observable.just(mockAccessToken));

        return new LoginPresenter(pocketAuthenticationApi, Schedulers.immediate(), Schedulers.immediate(),
                userStorage, DEFAULT_CONSUMER_KEY, DEFAULT_REDIRECT_URI);
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
    public void onLoginAction_retrievesRequestTokenAndStartsBrowser() throws Exception {
        presenterOnViewAttached();

        onLoginAction();

        verify(view).showLoadingView();

        final ArgumentCaptor<TypedInput> argument = ArgumentCaptor.forClass(TypedInput.class);
        verify(pocketAuthenticationApi).requestToken(argument.capture());
        assertEquals("{\"consumer_key\":\"" + DEFAULT_CONSUMER_KEY + "\",\"redirect_uri\":\"" + DEFAULT_REDIRECT_URI + "\"}\n", TestUtils.fromStream(argument.getValue().in()));

        verify(userStorage).storeRequestToken(DEFAULT_REQUEST_TOKEN);
        verify(view).hideLoadingView();

        verify(view).startBrowser(anyString());
    }

    @Test
    public void onLoginAction_pocketApiThrowsException_showRequestTokenError() throws Exception {
        when(pocketAuthenticationApi.requestToken(any())).thenThrow(RetrofitError.networkError("url", new IOException()));

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
        when(pocketAuthenticationApi.requestToken(any())).thenThrow(RetrofitError.networkError("url", new IOException())).thenReturn(Observable.just(mockRequestToken));

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
        when(pocketAuthenticationApi.accessToken(any())).thenThrow(RetrofitError.networkError("url", new IOException()));

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
