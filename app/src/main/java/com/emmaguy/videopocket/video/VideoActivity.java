package com.emmaguy.videopocket.video;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import com.emmaguy.videopocket.BasePresenter;
import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.storage.UserStorage;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import rx.Observable;
import rx.subjects.PublishSubject;

public class VideoActivity extends BaseActivity<VideoPresenter.View, VideoComponent> implements VideoPresenter.View {
    private final PublishSubject<SortOrder> mSortOrderSubject = PublishSubject.create();
    private final PublishSubject<Void> mRefreshSubject = PublishSubject.create();

    @Inject VideoPresenter mVideoPocketPresenter;
    @Inject UserStorage mUserStorage;

    @Bind(R.id.video_viewgroup_root) ViewGroup mViewGroupRoot;
    @Bind(R.id.video_recycler_view) RecyclerView mRecyclerView;
    @Bind(R.id.video_progressbar) ProgressBar mProgressBar;
    @Bind(R.id.video_toolbar) Toolbar mToolbar;

    private VideoAdapter mAdapter;

    public static void start(@NonNull final Context context) {
        context.startActivity(new Intent(context, VideoActivity.class));
    }

    @Override protected int getLayoutId() {
        return R.layout.activity_video;
    }

    @NonNull @Override protected BasePresenter<VideoPresenter.View> getPresenter() {
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

    @Override protected void onViewCreated(Bundle savedInstanceState) {
        mAdapter = new VideoAdapter();

        setSupportActionBar(mToolbar);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(ContextCompat.getDrawable(this, R.drawable.videos_divider)));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
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
        } else if (item.getItemId() == R.id.menu_sort_order) {
            final CharSequence sortOrders[] = new CharSequence[]{getString(R.string.sort_by_duration), getString(R.string.sort_by_time_added_to_pocket)};
            final SortOrder currentSortOrder = mUserStorage.getSortOrder();

            final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppThemeDialog);
            builder.setTitle(getString(R.string.sort_order));
            builder.setSingleChoiceItems(sortOrders, currentSortOrder.getIndex(), (dialog, index) -> {
                final SortOrder newSortOrder = SortOrder.fromIndex(index);
                if (currentSortOrder != newSortOrder) {
                    mSortOrderSubject.onNext(newSortOrder);
                }
                dialog.dismiss();
            });
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull @Override public Observable<Void> refreshAction() {
        return mRefreshSubject;
    }

    @NonNull @Override public Observable<SortOrder> sortOrderChanged() {
        return mSortOrderSubject;
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

    @Override public void showError() {
        Snackbar.make(mViewGroupRoot, R.string.error_generic_refresh, Snackbar.LENGTH_LONG).show();
    }
}
