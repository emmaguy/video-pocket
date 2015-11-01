package com.emmaguy.videopocket.feature.video;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.common.StringUtils;
import com.emmaguy.videopocket.common.Utils;
import com.emmaguy.videopocket.common.base.BasePresenter;
import com.emmaguy.videopocket.common.base.PresenterView;
import com.emmaguy.videopocket.storage.UserStorage;
import com.emmaguy.videopocket.storage.VideoStorage;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import timber.log.Timber;

class VideoPresenter extends BasePresenter<VideoPresenter.View> {
    private static final int POCKET_ARCHIVE_STATUS_SUCCESS = 1;

    private final YouTubeApi youTubeApi;
    private final PocketApi pocketApi;

    private final YouTubeParser youTubeParser;

    private final Scheduler ioScheduler;
    private final Scheduler uiScheduler;

    private final VideoStorage videoStorage;
    private final UserStorage userStorage;

    private final Resources resources;

    private final String youTubeApiKey;
    private final int youTubeRequestLimit;

    private boolean isRetrievingInProgress = false;

    VideoPresenter(@NonNull final PocketApi pocketApi, @NonNull final YouTubeApi youTubeApi, @NonNull final YouTubeParser youTubeParser,
                   @NonNull final Scheduler ioScheduler, @NonNull final Scheduler uiScheduler, @NonNull final VideoStorage videoStorage,
                   @NonNull final UserStorage userStorage, @NonNull final Resources resources,
                   @NonNull final String youTubeApiKey, final int youTubeRequestLimit) {
        this.pocketApi = pocketApi;
        this.youTubeApi = youTubeApi;
        this.youTubeParser = youTubeParser;
        this.ioScheduler = ioScheduler;
        this.uiScheduler = uiScheduler;
        this.videoStorage = videoStorage;
        this.userStorage = userStorage;
        this.resources = resources;
        this.youTubeApiKey = youTubeApiKey;
        this.youTubeRequestLimit = youTubeRequestLimit;
    }

    @Override public void onViewAttached(@NonNull final View view) {
        super.onViewAttached(view);

        isRetrievingInProgress = false;

        final List<Video> cachedVideos = videoStorage.getVideos();
        if (!cachedVideos.isEmpty()) {
            view.showVideos(cachedVideos);
        }

        final Observable<List<Video>> videoObservable = Observable.just(Utils.buildJson(new VideosRequestHolder(resources.getString(R.string.pocket_app_id), userStorage.getAccessToken(), "video", "simple")))
                .observeOn(uiScheduler)
                .doOnNext(typedInput -> view.showLoadingView())
                .observeOn(ioScheduler)
                .flatMap(typedInput -> Observable.defer(() -> pocketApi.videos(typedInput).map(map -> new ArrayList<>(map.values())))
                        .onErrorResumeNext(throwable -> Observable.just(new ArrayList<>()))
                        .observeOn(uiScheduler)
                        .doOnNext(pocketVideos -> {
                            if (pocketVideos.isEmpty()) {
                                view.showError();
                            }
                        })
                        .filter(pocketVideos -> pocketVideos != null))
                .flatMap(pocketVideos -> filterForYouTubeVideosAndRetrieveDurations(view, pocketVideos))
                .toSortedList((video, video2) -> {
                    final boolean sortedByDuration = userStorage.getSortOrder() == SortOrder.VIDEO_DURATION;
                    return sortedByDuration ? sortVideosByDuration(video, video2) : sortVideosById(video, video2);
                })
                .doOnNext(videoStorage::storeVideos)
                .observeOn(uiScheduler)
                .doOnNext(aVoid -> view.hideLoadingView());

        unsubscribeOnViewDetach(view.refreshAction().startWith(Observable.just(null))
                .filter(av -> !isRetrievingInProgress)
                .doOnNext(av -> isRetrievingInProgress = true)
                .flatMap(aVoid -> videoObservable)
                .doOnNext(av -> isRetrievingInProgress = false)
                .observeOn(uiScheduler)
                .subscribe(view::showVideos, throwable -> Timber.e(throwable, "Failed to get videos")));

        unsubscribeOnViewDetach(view.sortOrderChanged()
                .doOnNext(sortOrder -> view.showLoadingView())
                .doOnNext(userStorage::setSortOrder)
                .flatMap(sortOrder -> Observable.from(videoStorage.getVideos())
                        .toSortedList((video, video2) -> {
                            final boolean sortedByDuration = userStorage.getSortOrder() == SortOrder.VIDEO_DURATION;
                            return sortedByDuration ? sortVideosByDuration(video, video2) : sortVideosById(video, video2);
                        }))
                .doOnNext(videos -> view.hideLoadingView())
                .subscribe(view::showVideos, throwable -> Timber.e(throwable, "Failed to update videos with new sort order")));

        unsubscribeOnViewDetach(view.archiveAction()
                .doOnNext(videoDateTimePair -> view.showLoadingView())
                .observeOn(ioScheduler)
                .flatMap(videoDateTimePair -> Observable.defer(() ->
                        pocketApi.archive(resources.getString(R.string.pocket_app_id), userStorage.getAccessToken(), buildArchiveAction(videoDateTimePair.first, videoDateTimePair.second), "")
                                .map(actionResultResponse -> {
                                    if (actionResultResponse != null && actionResultResponse.status == POCKET_ARCHIVE_STATUS_SUCCESS) {
                                        return videoDateTimePair.first;
                                    }
                                    return null;
                                }))
                        .onErrorResumeNext(throwable -> Observable.just(null)))
                .observeOn(uiScheduler)
                .doOnNext(videoToArchive -> view.hideLoadingView())
                .filter(videoToArchive -> videoToArchive != null)
                .doOnNext(videoToArchive -> {
                    final List<Video> videos = videoStorage.getVideos();
                    videos.remove(videoToArchive);
                    videoStorage.storeVideos(videos);
                })
                .subscribe(view::archiveItem));

        unsubscribeOnViewDetach(view.searchQueryChanged()
                .flatMap(searchQuery -> Observable.from(videoStorage.getVideos())
                        .filter(video -> video.getTitle().toLowerCase().contains(searchQuery.toLowerCase()))
                        .toList())
                .observeOn(uiScheduler)
                .subscribe(view::showVideos, throwable -> Timber.e(throwable, "Failed to filter videos on search query")));
    }

    @NonNull
    private Observable<Video> filterForYouTubeVideosAndRetrieveDurations(final @NonNull View view, final @NonNull List<PocketVideo> pocketVideos) {
        return Observable.from(pocketVideos)
                .map(pocketVideo -> youTubeParser.getYouTubeId(pocketVideo.getUrl()))
                .filter(youTubeId -> !StringUtils.isEmpty(youTubeId))
                .buffer(youTubeRequestLimit)
                .flatMap(youTubeIds -> {
                    final HashMap<String, String> map = new HashMap<>();
                    map.put("id", StringUtils.join(",", youTubeIds));
                    return Observable.defer(() -> youTubeApi.videoData(map, youTubeApiKey)).onErrorResumeNext(throwable -> Observable.just(new ArrayList<>()));
                })
                .observeOn(uiScheduler)
                .doOnNext(youTubeVideos -> {
                    if (youTubeVideos.isEmpty()) {
                        view.showError();
                    }
                })
                .observeOn(ioScheduler)
                .flatMap(Observable::from)
                .filter(youTubeVideo -> youTubeVideo.getDuration() != null)
                .map(youTubeVideo -> {
                    for (PocketVideo pocketVideo : pocketVideos) {
                        if (pocketVideo.getUrl().contains(youTubeVideo.getId())) {
                            return new Video(pocketVideo.getId(), pocketVideo.getTitle(), pocketVideo.getUrl(), youTubeVideo.getDuration());
                        }
                    }
                    return null;
                }).filter(video -> video != null);
    }

    private int sortVideosByDuration(@NonNull final Video video, @NonNull final Video video2) {
        if (video.getDuration().getSeconds() > video2.getDuration().getSeconds()) {
            return -1;
        } else if (video.getDuration().getSeconds() < video2.getDuration().getSeconds()) {
            return 1;
        }
        return 0;
    }

    private int sortVideosById(@NonNull final Video video, @NonNull final Video video2) {
        if (video.getId() > video2.getId()) {
            return -1;
        } else if (video.getId() < video2.getId()) {
            return 1;
        }
        return 0;
    }

    @NonNull private String buildArchiveAction(@NonNull final Video video, final long now) {
        return "[" + new Gson().toJson(new ArchiveAction(String.valueOf(video.getId()), now)) + "]";
    }

    public interface View extends PresenterView {
        @NonNull Observable<Void> refreshAction();
        @NonNull Observable<String> searchQueryChanged();
        @NonNull Observable<SortOrder> sortOrderChanged();
        @NonNull Observable<Pair<Video, Long>> archiveAction();

        void archiveItem(final @NonNull Video videoToArchive);
        void showVideos(final @NonNull List<Video> videos);

        void showLoadingView();
        void hideLoadingView();

        void showError();
    }

    private static class ArchiveAction {
        @SerializedName("action") final String action = "archive";
        @SerializedName("item_id") final String itemId;
        @SerializedName("time") final long time;

        ArchiveAction(@NonNull final String itemId, final long time) {
            this.itemId = itemId;
            this.time = time;
        }
    }

    private static class VideosRequestHolder {
        @SerializedName("consumer_key") final String consumerKey;
        @SerializedName("access_token") final String accessToken;
        @SerializedName("contentType") final String contentType;
        @SerializedName("detailType") final String detailType;

        VideosRequestHolder(@NonNull final String consumerKey, @NonNull final String accessToken, @NonNull final String contentType, @NonNull final String detailType) {
            this.consumerKey = consumerKey;
            this.accessToken = accessToken;
            this.contentType = contentType;
            this.detailType = detailType;
        }
    }
}
