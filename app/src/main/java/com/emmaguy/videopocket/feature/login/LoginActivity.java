package com.emmaguy.videopocket.feature.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.emmaguy.videopocket.feature.ActivityComponent;
import com.emmaguy.videopocket.common.base.BaseActivity;
import com.emmaguy.videopocket.common.base.BasePresenter;
import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.feature.video.VideoActivity;
import com.jakewharton.rxbinding.view.RxView;

import javax.inject.Inject;

import butterknife.Bind;
import rx.Observable;
import rx.subjects.BehaviorSubject;

public class LoginActivity extends BaseActivity<LoginPresenter.View, LoginComponent> implements LoginPresenter.View {
    private final BehaviorSubject<Void> returnedFromBrowserSubject = BehaviorSubject.create();

    @Inject LoginPresenter loginPresenter;

    @Bind(R.id.login_viewgroup_root) ViewGroup rootViewGroup;
    @Bind(R.id.login_progress_bar) ProgressBar progressBar;
    @Bind(R.id.login_button) Button loginButton;
    @Bind(R.id.toolbar) Toolbar toolbar;

    @Override protected void onViewCreated(Bundle savedInstanceState) {
        super.onViewCreated(savedInstanceState);

        setSupportActionBar(toolbar);
    }

    @Override protected int getLayoutId() {
        return R.layout.activity_login;
    }

    @NonNull @Override protected BasePresenter<LoginPresenter.View> getPresenter() {
        return loginPresenter;
    }

    @NonNull @Override protected LoginPresenter.View getPresenterView() {
        return this;
    }

    @NonNull @Override protected LoginComponent createComponent(@NonNull final ActivityComponent component) {
        return component.plus(new LoginModule());
    }

    @Override protected void inject(@NonNull final LoginComponent component) {
        component.inject(this);
    }

    @Override protected void onResume() {
        super.onResume();

        final Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(getString(R.string.callback_url_scheme))) {
            returnedFromBrowserSubject.onNext(null);
        }
    }

    @Override public void showLoadingView() {
        progressBar.setVisibility(android.view.View.VISIBLE);
    }

    @Override public void hideLoadingView() {
        progressBar.setVisibility(android.view.View.GONE);
    }

    @Override public void showRequestTokenError() {
        Snackbar.make(rootViewGroup, R.string.failed_to_retrieve_request_token, Snackbar.LENGTH_LONG).show();
    }

    @Override public void showAccessTokenError() {
        Snackbar.make(rootViewGroup, R.string.failed_to_retrieve_access_token, Snackbar.LENGTH_LONG).show();
    }

    @NonNull @Override public Observable<Void> retrieveRequestToken() {
        return RxView.clicks(loginButton).map(o -> null);
    }

    @NonNull @Override public Observable<Void> returnFromBrowser() {
        return returnedFromBrowserSubject;
    }

    @Override public void startBrowser(@NonNull final String url) {
        Snackbar.make(rootViewGroup, R.string.redirecting_to_browser, Snackbar.LENGTH_LONG).show();

        finish();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    @Override public void startVideos() {
        VideoActivity.start(this);
    }
}
