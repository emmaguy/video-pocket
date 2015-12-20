package com.emmaguy.videopocket.feature.video;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.common.Results;
import com.emmaguy.videopocket.common.StringUtils;
import com.emmaguy.videopocket.common.base.BasePresenter;
import com.emmaguy.videopocket.common.base.PresenterView;
import com.emmaguy.videopocket.storage.UserStorage;
import com.emmaguy.videopocket.storage.VideoStorage;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import rx.Observable;
import rx.Scheduler;
import timber.log.Timber;

class VideoPresenter extends BasePresenter<VideoPresenter.View> {
    private static final int POCKET_ARCHIVE_STATUS_SUCCESS = 1;
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

    private final YouTubeApi youTubeApi;
    private final PocketApi pocketApi;

    private final YouTubeParser youTubeParser;

    private final Scheduler ioScheduler;
    private final Scheduler uiScheduler;

    private final VideoStorage videoStorage;
    private final UserStorage userStorage;

    private final Resources resources;
    private final Gson gson;

    private final String youTubeApiKey;
    private final int youTubeRequestLimit;

    private boolean isRetrievingInProgress = false;

    VideoPresenter(@NonNull final PocketApi pocketApi, @NonNull final YouTubeApi youTubeApi, @NonNull final YouTubeParser youTubeParser,
                   @NonNull final Scheduler ioScheduler, @NonNull final Scheduler uiScheduler, @NonNull final VideoStorage videoStorage,
                   @NonNull final UserStorage userStorage, @NonNull final Resources resources, @NonNull final Gson gson,
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
        this.gson = gson;
    }

    @Override public void onViewAttached(@NonNull final View view) {
        super.onViewAttached(view);

        isRetrievingInProgress = false;

        final List<Video> cachedVideos = videoStorage.getVideos();
        if (!cachedVideos.isEmpty()) {
            view.showVideos(cachedVideos);
        }

        final String json = gson.toJson(new VideosRequestHolder(resources.getString(R.string.pocket_app_id), userStorage.getAccessToken(), "video", "simple"));
        final RequestBody body = RequestBody.create(MEDIA_TYPE, json);
        final Observable<List<Video>> videoObservable =
                view.refreshAction().startWith(Observable.just(null))
                        .filter(aVoid -> !isRetrievingInProgress)
                        .doOnNext(aVoid -> isRetrievingInProgress = true)
                        .flatMap(aVoid -> Observable.just(body)
                                .observeOn(uiScheduler)
                                .doOnNext(typedInput -> view.showLoadingView())
                                .observeOn(ioScheduler)
                                .flatMap(requestBody -> retrieveVideosFromPocket(view, requestBody))
                                .map(VideoGrouper.groupBySource())
                                .flatMap(videosBySource -> {
                                    final List<Observable<Video>> videoRetrievingObservables = new ArrayList<>();
                                    final TreeMap<Integer, Collection<String>> otherSources = new TreeMap<>();

                                    for (Map.Entry<String, Collection<PocketVideo>> videoMapEntry : videosBySource.entrySet()) {
                                        if (videoMapEntry.getKey().contains("youtube")) {
                                            videoRetrievingObservables.add(filterNonYouTubeVideosAndRetrieveDurations(view, videoMapEntry.getValue()));
                                        } else {
                                            int size = videoMapEntry.getValue().size();
                                            if (!otherSources.containsKey(size)) {
                                                otherSources.put(size, new ArrayList<>());
                                            }
                                            otherSources.get(size).add(videoMapEntry.getKey());
                                        }
                                    }

                                    userStorage.storeOtherSources(otherSources.descendingMap());
                                    return Observable.merge(videoRetrievingObservables);
                                })
                                .toSortedList(VideoSorter.sort(userStorage))
                                .doOnNext(videoStorage::storeVideos)
                                .observeOn(uiScheduler)
                                .doOnNext(avoid -> view.hideLoadingView()))
                        .doOnNext(av -> isRetrievingInProgress = false);

        unsubscribeOnViewDetach(Observable.combineLatest(videoObservable,
                view.searchQueryChanged().startWith(""), Pair::create)
                .flatMap(pair -> Observable.from(pair.first)
                        .filter(video -> video.getTitle().toLowerCase().contains(pair.second.toLowerCase()))
                        .toList())
                .observeOn(uiScheduler)
                .subscribe(view::showVideos, throwable -> Timber.e(throwable, "Failed show videos")));

        unsubscribeOnViewDetach(view.sortOrderChanged()
                .doOnNext(sortOrder -> view.showLoadingView())
                .doOnNext(userStorage::setSortOrder)
                .flatMap(sortOrder -> Observable.from(videoStorage.getVideos())
                        .toSortedList(VideoSorter.sort(userStorage)))
                .doOnNext(videos -> view.hideLoadingView())
                .subscribe(view::showVideos, throwable -> Timber.e(throwable, "Failed to update videos with new sort order")));

        unsubscribeOnViewDetach(view.archiveAction()
                .doOnNext(videoDateTimePair -> view.showLoadingView())
                .observeOn(ioScheduler)
                .flatMap(videoDateTimePair -> {
                    final String action = "[" + gson.toJson(new ArchiveAction(String.valueOf(videoDateTimePair.first.getId()), videoDateTimePair.second)) + "]";
                    return pocketApi.archive(resources.getString(R.string.pocket_app_id), userStorage.getAccessToken(), action)
                            .map(result -> {
                                if (Results.isSuccess(result) && result.response().body().status == POCKET_ARCHIVE_STATUS_SUCCESS) {
                                    return videoDateTimePair.first;
                                }
                                return null;
                            });
                })
                .observeOn(uiScheduler)
                .doOnNext(videoToArchive -> view.hideLoadingView())
                .filter(videoToArchive -> videoToArchive != null)
                .doOnNext(videoToArchive -> {
                    final List<Video> videos = videoStorage.getVideos();
                    videos.remove(videoToArchive);
                    videoStorage.storeVideos(videos);
                })
                .subscribe(view::archiveItem, throwable -> Timber.e(throwable, "Failed to archive video")));

        unsubscribeOnViewDetach(view.otherSourcesAction()
                .map(aVoid -> {
                    final StringBuilder builder = new StringBuilder();
                    builder.append(resources.getString(R.string.other_source_intro));
                    for (Map.Entry<Integer, Collection<String>> item : userStorage.getOtherSources().entrySet()) {
                        builder.append(resources.getString(R.string.other_sources_format, item.getKey(), StringUtils.join(", ", item.getValue())));
                    }

                    return builder.toString();
                })
                .subscribe(view::showOtherSources, throwable -> Timber.e(throwable, "Failed to get other sources")));
    }

    @NonNull
    private Observable<ArrayList<PocketVideo>> retrieveVideosFromPocket(@NonNull final View view, @NonNull final RequestBody requestBody) {
        return pocketApi.videos(requestBody)
                .observeOn(uiScheduler)
                .doOnNext(result -> {
                    if (!Results.isSuccess(result)) {
                        view.showError();
                    }
                })
                .observeOn(ioScheduler)
                .filter(Results::isSuccess)
                .map(result -> new ArrayList<>(result.response().body().getMap().values()))
                .map(pocketVideos -> {
                    if (pocketVideos.isEmpty()) {
                        view.showError();
                        return null;
                    }
                    return pocketVideos;
                })
                .filter(pocketVideos -> pocketVideos != null);
    }

    @NonNull
    private Observable<Video> filterNonYouTubeVideosAndRetrieveDurations(@NonNull final View view, @NonNull final Collection<PocketVideo> pocketVideos) {
        return Observable.from(pocketVideos)
                .map(pocketVideo -> youTubeParser.getYouTubeId(pocketVideo.getUrl()))
                .filter(youTubeId -> !StringUtils.isEmpty(youTubeId))
                .buffer(youTubeRequestLimit)
                .observeOn(ioScheduler)
                .flatMap(youTubeIds -> {
                    final HashMap<String, String> map = new HashMap<>();
                    map.put("id", StringUtils.join(",", youTubeIds));
                    return youTubeApi.videoData(map, youTubeApiKey);
                })
                .observeOn(uiScheduler)
                .doOnNext(result -> {
                    if (!Results.isSuccess(result)) {
                        view.showError();
                    }
                })
                .observeOn(ioScheduler)
                .filter(Results::isSuccess)
                .map(result -> result.response().body().getItems())
                .flatMap(Observable::from)
                .filter(r -> r.getContentDetails() != null)
                .map(r -> new YouTubeVideo(r.getId(), r.getContentDetails().getDuration(), r.getStatistics().getViewCount()))
                .map(youTubeVideo -> {
                    for (PocketVideo pocketVideo : pocketVideos) {
                        if (pocketVideo.getUrl().contains(youTubeVideo.getId())) {
                            return new Video(pocketVideo.getId(), pocketVideo.getTitle(), pocketVideo.getUrl(),
                                    youTubeVideo.getDuration(), youTubeVideo.getViewCount());
                        }
                    }
                    return null;
                }).filter(video -> video != null);
    }

    public interface View extends PresenterView {
        @NonNull Observable<Void> refreshAction();
        @NonNull Observable<String> searchQueryChanged();
        @NonNull Observable<SortOrder> sortOrderChanged();
        @NonNull Observable<Pair<Video, Long>> archiveAction();
        @NonNull Observable<Void> otherSourcesAction();

        void archiveItem(@NonNull final Video videoToArchive);
        void showVideos(@NonNull final List<Video> videos);

        void showLoadingView();
        void hideLoadingView();

        void showError();
        void showOtherSources(@NonNull final String otherSources);
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
