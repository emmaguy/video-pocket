package com.emmaguy.videopocket.login;

import com.emmaguy.videopocket.PresenterTest;
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

public class LoginPresenterTest extends PresenterTest<LoginPresenter, LoginPresenter.View> {
    private static final String DEFAULT_REQUEST_TOKEN = "test_request_token";
    private static final String DEFAULT_ACCESS_TOKEN = "test_access_token";
    private static final String DEFAULT_CONSUMER_KEY = "test_consumer_key";
    private static final String DEFAULT_REDIRECT_URI = "test://callback";
    private static final String DEFAULT_USERNAME = "test_username";

    @Mock private PocketAuthenticationApi mPocketApi;
    @Mock private UserStorage mUserStorage;

    private final PublishSubject<Void> mReturnFromBrowserSubject = PublishSubject.create();
    private final PublishSubject<Void> mLoginSubject = PublishSubject.create();

    @Override protected LoginPresenter createPresenter() {
        when(mUserStorage.getAccessToken()).thenReturn("");

        final RequestToken mockRequestToken = mock(RequestToken.class);
        when(mockRequestToken.getCode()).thenReturn(DEFAULT_REQUEST_TOKEN);

        when(mPocketApi.requestToken(any())).thenReturn(Observable.just(mockRequestToken));

        final AccessToken mockAccessToken = mock(AccessToken.class);
        when(mockAccessToken.getUsername()).thenReturn(DEFAULT_USERNAME);
        when(mockAccessToken.getAccessToken()).thenReturn(DEFAULT_ACCESS_TOKEN);
        when(mPocketApi.accessToken(any())).thenReturn(Observable.just(mockAccessToken));

        return new LoginPresenter(mPocketApi, Schedulers.immediate(), Schedulers.immediate(), mUserStorage, DEFAULT_CONSUMER_KEY, DEFAULT_REDIRECT_URI);
    }

    @Override protected LoginPresenter.View createView() {
        final LoginPresenter.View view = mock(LoginPresenter.View.class);
        when(view.retrieveRequestTokenObservable()).thenReturn(mLoginSubject);
        when(view.returnFromBrowserObservable()).thenReturn(mReturnFromBrowserSubject);
        return view;
    }

    @Test
    public void onViewAttached_whenHasAccessToken_shouldStartVideos() throws Exception {
        when(mUserStorage.getAccessToken()).thenReturn(DEFAULT_ACCESS_TOKEN);

        presenterOnViewAttached();

        verify(mView).startVideos();
        verifyNoMoreInteractions(mView);

        verifyZeroInteractions(mPocketApi);
    }

    @Test
    public void onViewAttached_whenHasNoAccessToken_subscribesToLoginActionAndReturnFromBrowserAction() throws Exception {
        presenterOnViewAttached();

        verify(mView).retrieveRequestTokenObservable();
        verify(mView).returnFromBrowserObservable();
    }

    @Test
    public void onLoginAction_retrievesRequestTokenAndStartsBrowser() throws Exception {
        presenterOnViewAttached();

        onLoginAction();

        verify(mView).showLoadingView();

        final ArgumentCaptor<TypedInput> argument = ArgumentCaptor.forClass(TypedInput.class);
        verify(mPocketApi).requestToken(argument.capture());
        assertEquals("{\"consumer_key\":\"" + DEFAULT_CONSUMER_KEY + "\",\"redirect_uri\":\"" + DEFAULT_REDIRECT_URI + "\"}\n", TestUtils.fromStream(argument.getValue().in()));

        verify(mUserStorage).storeRequestToken(DEFAULT_REQUEST_TOKEN);
        verify(mView).hideLoadingView();

        verify(mView).startBrowser(anyString());
    }

    @Test
    public void onLoginAction_pocketApiThrowsException_showRequestTokenError() throws Exception {
        when(mPocketApi.requestToken(any())).thenThrow(RetrofitError.networkError("url", new IOException()));

        presenterOnViewAttached();
        onLoginAction();

        verify(mView, never()).startBrowser(anyString());

        verify(mView).hideLoadingView();
        verify(mView).showRequestTokenError();
    }

    @Test
    public void afterErrorWithOnLoginAction_onLoginActionWithValidResponse_startsBrowser() throws Exception {
        final RequestToken mockRequestToken = mock(RequestToken.class);
        when(mockRequestToken.getCode()).thenReturn(DEFAULT_REQUEST_TOKEN);
        when(mPocketApi.requestToken(any())).thenThrow(RetrofitError.networkError("url", new IOException())).thenReturn(Observable.just(mockRequestToken));

        presenterOnViewAttached();
        onLoginAction();

        onLoginAction();

        verify(mView).startBrowser(anyString());
    }

    @Test
    public void onReturnFromBrowserAction_startVideos() throws Exception {
        presenterOnViewAttached();

        onReturnFromBrowserAction();

        verify(mView).showLoadingView();

        verify(mUserStorage).storeUsername(DEFAULT_USERNAME);
        verify(mUserStorage).storeAccessToken(DEFAULT_ACCESS_TOKEN);
        verify(mUserStorage).storeRequestToken("");

        verify(mView).hideLoadingView();

        verify(mView).startVideos();
    }

    @Test
    public void onReturnFromBrowserAction_pocketApiThrowsException_showAccessTokenError() throws Exception {
        when(mPocketApi.accessToken(any())).thenThrow(RetrofitError.networkError("url", new IOException()));

        presenterOnViewAttached();
        onReturnFromBrowserAction();

        verify(mView, never()).startVideos();

        verify(mView).hideLoadingView();
        verify(mView).showAccessTokenError();
    }

    private void onLoginAction() {
        mLoginSubject.onNext(null);
    }

    private void onReturnFromBrowserAction() {
        mReturnFromBrowserSubject.onNext(null);
    }
}
