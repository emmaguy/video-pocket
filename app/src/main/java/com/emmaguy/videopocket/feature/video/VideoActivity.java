package com.emmaguy.videopocket.feature.video;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.emmaguy.videopocket.feature.ActivityComponent;
import com.emmaguy.videopocket.common.base.BaseActivity;
import com.emmaguy.videopocket.common.base.BasePresenter;
import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.storage.UserStorage;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import rx.Observable;
import rx.subjects.PublishSubject;

public class VideoActivity extends BaseActivity<VideoPresenter.View, VideoComponent> implements VideoPresenter.View, SearchView.OnQueryTextListener {
    private final PublishSubject<Pair<Video, Long>> mArchiveSubject = PublishSubject.create();
    private final PublishSubject<SortOrder> mSortOrderSubject = PublishSubject.create();
    private final PublishSubject<String> mSearchQuerySubject = PublishSubject.create();
    private final PublishSubject<Void> mRefreshSubject = PublishSubject.create();

    @Inject VideoPresenter mVideoPocketPresenter;
    @Inject UserStorage mUserStorage;

    @Bind(R.id.video_viewgroup_root) ViewGroup mViewGroupRoot;
    @Bind(R.id.video_recycler_view) RecyclerView mRecyclerView;
    @Bind(R.id.video_progressbar) ProgressBar mProgressBar;
    @Bind(R.id.toolbar) Toolbar mToolbar;

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

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override public int getMovementFlags(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder) {
                return makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.START) | makeFlag(ItemTouchHelper.ACTION_STATE_SWIPE, ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public boolean onMove(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, final RecyclerView.ViewHolder target) {
                return false;
            }

            @Override public void onSwiped(final RecyclerView.ViewHolder viewHolder, final int direction) {
                final VideoAdapter.ViewHolder holder = (VideoAdapter.ViewHolder) viewHolder;
                mArchiveSubject.onNext(new Pair<>(mAdapter.getItemAt(holder.getAdapterPosition()), LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)));
            }
        });
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_video, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null) {
            final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            final SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setOnQueryTextListener(this);
        }

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

    @NonNull @Override public Observable<String> searchQueryChanged() {
        return mSearchQuerySubject;
    }

    @NonNull @Override public Observable<SortOrder> sortOrderChanged() {
        return mSortOrderSubject;
    }

    @NonNull @Override public Observable<Pair<Video, Long>> archiveAction() {
        return mArchiveSubject;
    }

    @Override public void archiveItem(final @NonNull Video video) {
        Snackbar.make(mViewGroupRoot, R.string.video_moved_to_archive, Snackbar.LENGTH_LONG).show();
        mAdapter.removeVideo(video);
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
        Snackbar.make(mViewGroupRoot, R.string.something_went_wrong_whilst_refreshing, Snackbar.LENGTH_LONG).show();
    }

    @Override public boolean onQueryTextSubmit(final String query) {
        return true;
    }

    @Override public boolean onQueryTextChange(final String newText) {
        mSearchQuerySubject.onNext(newText);
        return true;
    }
}
