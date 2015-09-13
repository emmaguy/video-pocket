package com.emmaguy.videopocket.video;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.emmaguy.videopocket.ActivityComponent;
import com.emmaguy.videopocket.BaseActivity;
import com.emmaguy.videopocket.Presenter;
import com.emmaguy.videopocket.R;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import rx.Observable;
import rx.subjects.PublishSubject;

public class VideoActivity extends BaseActivity<VideoPresenter.View, VideoComponent> implements VideoPresenter.View {
    @Inject VideoPresenter mVideoPocketPresenter;

    private final PublishSubject<Void> mRefreshSubject = PublishSubject.create();

    private VideoAdapter mAdapter;

    @Bind(R.id.video_viewgroup_root) ViewGroup mViewGroupRoot;
    @Bind(R.id.video_recycler_view) RecyclerView mRecyclerView;
    @Bind(R.id.video_progressbar) ProgressBar mProgressBar;
    @Bind(R.id.video_toolbar) Toolbar mToolbar;

    @Override protected int getLayoutId() {
        return R.layout.activity_video;
    }

    @Override protected void onViewCreated(Bundle savedInstanceState) {
        mAdapter = new VideoAdapter();

        setSupportActionBar(mToolbar);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(ContextCompat.getDrawable(this, R.drawable.videos_divider)));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @NonNull @Override protected Presenter<VideoPresenter.View> getPresenter() {
        return mVideoPocketPresenter;
    }

    @NonNull @Override protected VideoPresenter.View getPresenterView() {
        return this;
    }

    @NonNull @Override protected VideoComponent createComponent(@NonNull final ActivityComponent component) {
        return component.plus(new VideoModule());
    }

    @Override protected void inject(@NonNull final VideoComponent component) {
        component.inject(this);
    }

    @Override public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_video, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_video_refresh) {
            mRefreshSubject.onNext(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void showError() {
        Snackbar.make(mViewGroupRoot, R.string.error_generic_refresh, Snackbar.LENGTH_LONG).show();
    }

    @NonNull @Override public Observable<Void> refreshActionObservable() {
        return mRefreshSubject;
    }

    @Override public void showVideos(@NonNull final List<Video> videos) {
        mAdapter.updateVideos(videos);
    }

    @Override public void showLoadingView() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override public void hideLoadingView() {
        mProgressBar.setVisibility(View.GONE);
    }

    public static void start(@NonNull final Context context) {
        context.startActivity(new Intent(context, VideoActivity.class));
    }
}
