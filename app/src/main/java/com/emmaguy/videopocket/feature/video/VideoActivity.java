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

import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.common.base.BaseActivity;
import com.emmaguy.videopocket.common.base.BasePresenter;
import com.emmaguy.videopocket.feature.ActivityComponent;
import com.emmaguy.videopocket.storage.UserStorage;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import rx.Observable;
import rx.subjects.PublishSubject;

public class VideoActivity extends BaseActivity<VideoPresenter.View, VideoComponent> implements VideoPresenter.View, SearchView.OnQueryTextListener {
    private final PublishSubject<Pair<Video, Long>> archiveSubject = PublishSubject.create();
    private final PublishSubject<SortOrder> sortOrderSubject = PublishSubject.create();
    private final PublishSubject<String> searchQuerySubject = PublishSubject.create();
    private final PublishSubject<Void> otherSourcesSubject = PublishSubject.create();
    private final PublishSubject<Void> refreshSubject = PublishSubject.create();

    @Inject VideoPresenter videoPresenter;
    @Inject UserStorage userStorage;

    @Bind(R.id.video_viewgroup_root) ViewGroup rootViewGroup;
    @Bind(R.id.video_recycler_view) RecyclerView recyclerView;
    @Bind(R.id.video_progressbar) ProgressBar progressBar;
    @Bind(R.id.toolbar) Toolbar toolbar;

    private VideoAdapter adapter;

    public static void start(@NonNull final Context context) {
        context.startActivity(new Intent(context, VideoActivity.class));
    }

    @Override protected int getLayoutId() {
        return R.layout.activity_video;
    }

    @NonNull @Override protected BasePresenter<VideoPresenter.View> getPresenter() {
        return videoPresenter;
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
        adapter = new VideoAdapter();

        setSupportActionBar(toolbar);

        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(ContextCompat.getDrawable(this, R.drawable.videos_divider)));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder) {
                return makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.START) | makeFlag(ItemTouchHelper.ACTION_STATE_SWIPE, ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public boolean onMove(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, final RecyclerView.ViewHolder target) {
                return false;
            }

            @Override public void onSwiped(final RecyclerView.ViewHolder viewHolder, final int direction) {
                final VideoAdapter.ViewHolder holder = (VideoAdapter.ViewHolder) viewHolder;
                archiveSubject.onNext(new Pair<>(adapter.getItemAt(holder.getAdapterPosition()), LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)));
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
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
            refreshSubject.onNext(null);
            return true;
        } else if (item.getItemId() == R.id.menu_sort_order) {
            final CharSequence sortOrders[] = new CharSequence[]{getString(R.string.sort_by_duration), getString(R.string.sort_by_time_added_to_pocket)};
            final SortOrder currentSortOrder = userStorage.getSortOrder();

            final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppThemeDialog);
            builder.setTitle(getString(R.string.sort_order));
            builder.setSingleChoiceItems(sortOrders, currentSortOrder.getIndex(), (dialog, index) -> {
                final SortOrder newSortOrder = SortOrder.fromIndex(index);
                if (currentSortOrder != newSortOrder) {
                    sortOrderSubject.onNext(newSortOrder);
                }
                dialog.dismiss();
            });
            builder.show();
            return true;
        } else if (item.getItemId() == R.id.menu_other_sources) {
            otherSourcesSubject.onNext(null);
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull @Override public Observable<Void> refreshAction() {
        return refreshSubject;
    }

    @NonNull @Override public Observable<String> searchQueryChanged() {
        return searchQuerySubject;
    }

    @NonNull @Override public Observable<SortOrder> sortOrderChanged() {
        return sortOrderSubject;
    }

    @NonNull @Override public Observable<Pair<Video, Long>> archiveAction() {
        return archiveSubject;
    }

    @NonNull @Override public Observable<Void> otherSourcesAction() {
        return otherSourcesSubject;
    }

    @Override public void archiveItem(@NonNull final Video video) {
        Snackbar.make(rootViewGroup, R.string.video_moved_to_archive, Snackbar.LENGTH_LONG).show();
        adapter.removeVideo(video);
    }

    @Override public void showVideos(@NonNull final List<Video> videos) {
        adapter.updateVideos(videos);
    }

    @Override public void showLoadingView() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override public void hideLoadingView() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override public void showError() {
        Snackbar.make(rootViewGroup, R.string.something_went_wrong_whilst_refreshing, Snackbar.LENGTH_LONG).show();
    }

    @Override public void showOtherSources(@NonNull final String otherSources) {
        new AlertDialog.Builder(this, R.style.AppThemeDialog)
                .setTitle(R.string.other_sources)
                .setMessage(otherSources)
                .show();
    }

    @Override public boolean onQueryTextSubmit(final String query) {
        return true;
    }

    @Override public boolean onQueryTextChange(final String newText) {
        searchQuerySubject.onNext(newText);
        return true;
    }
}
