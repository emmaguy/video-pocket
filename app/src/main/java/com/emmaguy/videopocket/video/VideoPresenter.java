package com.emmaguy.videopocket.video;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.emmaguy.videopocket.BasePresenter;
import com.emmaguy.videopocket.PresenterView;
import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.StringUtils;
import com.emmaguy.videopocket.Utils;
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

    private final YouTubeApi mYouTubeApi;
    private final PocketApi mPocketApi;

    private final YouTubeParser mYouTubeParser;

    private final Scheduler mIoScheduler;
    private final Scheduler mUiScheduler;

    private final VideoStorage mVideoStorage;
    private final UserStorage mUserStorage;

    private final Resources mResources;

    private final String mYouTubeApiKey;
    private final int mYouTubeRequestLimit;

    private boolean mIsRetrievingInProgress = false;

    VideoPresenter(@NonNull final PocketApi pocketApi, @NonNull final YouTubeApi youTubeApi, @NonNull final YouTubeParser youTubeParser,
                   @NonNull final Scheduler ioScheduler, @NonNull final Scheduler uiScheduler, @NonNull final VideoStorage videoStorage,
                   @NonNull final UserStorage userStorage, @NonNull final Resources resources,
                   @NonNull final String youTubeApiKey, final int youTubeRequestLimit) {
        mPocketApi = pocketApi;
        mYouTubeApi = youTubeApi;
        mYouTubeParser = youTubeParser;
        mIoScheduler = ioScheduler;
        mUiScheduler = uiScheduler;
        mVideoStorage = videoStorage;
        mUserStorage = userStorage;
        mResources = resources;
        mYouTubeApiKey = youTubeApiKey;
        mYouTubeRequestLimit = youTubeRequestLimit;
    }

    @Override
    public void onViewAttached(@NonNull final View view) {
        super.onViewAttached(view);

        // We unsubscribe when the view is detached
        mIsRetrievingInProgress = false;

        final List<Video> cachedVideos = mVideoStorage.getVideos();
        if (!cachedVideos.isEmpty()) {
            view.showVideos(cachedVideos);
        }

        final Observable<List<Video>> videoObservable = Observable.just(Utils.buildJson(new VideosRequestHolder(mResources.getString(R.string.pocket_app_id), mUserStorage.getAccessToken(), "video", "simple")))
                .observeOn(mUiScheduler)
                .doOnNext(typedInput -> view.showLoadingView())
                .observeOn(mIoScheduler)
                .flatMap(typedInput -> Observable.defer(() -> mPocketApi.videos(typedInput).map(map -> new ArrayList<>(map.values())))
                        .onErrorResumeNext(throwable -> Observable.just(new ArrayList<>()))
                        .observeOn(mUiScheduler)
                        .doOnNext(pocketVideos -> {
                            if (pocketVideos.isEmpty()) {
                                view.showError();
                            }
                        })
                        .filter(pocketVideos -> pocketVideos != null))
                .flatMap(pocketVideos -> filterForYouTubeVideosAndRetrieveDurations(view, pocketVideos))
                .toSortedList((video, video2) -> mUserStorage.getSortOrder() == SortOrder.VIDEO_DURATION ? sortVideosByDuration(video, video2) : sortVideosById(video, video2))
                .doOnNext(mVideoStorage::storeVideos)
                .observeOn(mUiScheduler)
                .doOnNext(aVoid -> view.hideLoadingView());

        unsubscribeOnViewDetach(view.refreshAction().startWith(Observable.just(null))
                .filter(av -> !mIsRetrievingInProgress)
                .doOnNext(av -> mIsRetrievingInProgress = true)
                .flatMap(aVoid -> videoObservable)
                .doOnNext(av -> mIsRetrievingInProgress = false)
                .observeOn(mUiScheduler)
                .subscribe(view::showVideos, throwable -> Timber.e(throwable, "Failed to get videos")));

        unsubscribeOnViewDetach(view.sortOrderChanged()
                .doOnNext(sortOrder -> view.showLoadingView())
                .doOnNext(mUserStorage::setSortOrder)
                .flatMap(sortOrder -> Observable.from(mVideoStorage.getVideos())
                        .toSortedList((video, video2) -> mUserStorage.getSortOrder() == SortOrder.VIDEO_DURATION ? sortVideosByDuration(video, video2) : sortVideosById(video, video2)))
                .doOnNext(videos -> view.hideLoadingView())
                .subscribe(view::showVideos, throwable -> Timber.e(throwable, "Failed to update videos with new sort order")));

        unsubscribeOnViewDetach(view.archiveAction()
                .doOnNext(videoDateTimePair -> view.showLoadingView())
                .observeOn(mIoScheduler)
                .flatMap(videoDateTimePair -> Observable.defer(() ->
                        mPocketApi.archive(mResources.getString(R.string.pocket_app_id), mUserStorage.getAccessToken(), buildArchiveAction(videoDateTimePair.first, videoDateTimePair.second), "")
                                .map(actionResultResponse -> {
                                    if (actionResultResponse != null && actionResultResponse.mStatus == POCKET_ARCHIVE_STATUS_SUCCESS) {
                                        return videoDateTimePair.first;
                                    }
                                    return null;
                                }))
                        .onErrorResumeNext(throwable -> Observable.just(null)))
                .observeOn(mUiScheduler)
                .doOnNext(videoToArchive -> view.hideLoadingView())
                .filter(videoToArchive -> videoToArchive != null)
                .doOnNext(videoToArchive -> {
                    final List<Video> videos = mVideoStorage.getVideos();
                    videos.remove(videoToArchive);
                    mVideoStorage.storeVideos(videos);
                })
                .subscribe(view::archiveItem));
    }

    @NonNull
    private Observable<Video> filterForYouTubeVideosAndRetrieveDurations(final @NonNull View view, final @NonNull List<PocketVideo> pocketVideos) {
        return Observable.from(pocketVideos)
                .map(pocketVideo -> mYouTubeParser.getYouTubeId(pocketVideo.getUrl()))
                .filter(youTubeId -> !StringUtils.isEmpty(youTubeId))
                .buffer(mYouTubeRequestLimit)
                .flatMap(youTubeIds -> {
                    final HashMap<String, String> map = new HashMap<>();
                    map.put("id", StringUtils.join(",", youTubeIds));
                    return Observable.defer(() -> mYouTubeApi.videoData(map, mYouTubeApiKey)).onErrorResumeNext(throwable -> Observable.just(new ArrayList<>()));
                })
                .observeOn(mUiScheduler)
                .doOnNext(youTubeVideos -> {
                    if (youTubeVideos.isEmpty()) {
                        view.showError();
                    }
                })
                .observeOn(mIoScheduler)
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

    @NonNull private String buildArchiveAction(@NonNull final Video video, final long  now) {
        return "[" + new Gson().toJson(new ArchiveAction(String.valueOf(video.getId()), now)) + "]";
    }

    public interface View extends PresenterView {
        @NonNull Observable<Void> refreshAction();
        @NonNull Observable<SortOrder> sortOrderChanged();
        @NonNull Observable<Pair<Video, Long>> archiveAction();

        void archiveItem(final @NonNull Video videoToArchive);
        void showVideos(final @NonNull List<Video> videos);

        void showLoadingView();
        void hideLoadingView();

        void showError();
    }

    private static class ArchiveAction {
        @SerializedName("action") final String mAction = "archive";
        @SerializedName("item_id") final String mItemId;
        @SerializedName("time") final long mTime;

        ArchiveAction(@NonNull final String itemId, final long now) {
            mItemId = itemId;
            mTime = now;
        }
    }

    private static class VideosRequestHolder {
        @SerializedName("consumer_key") final String mConsumerKey;
        @SerializedName("access_token") final String mAccessToken;
        @SerializedName("contentType") final String mContentType;
        @SerializedName("detailType") final String mDetailType;

        VideosRequestHolder(@NonNull final String consumerKey, @NonNull final String accessToken, @NonNull final String contentType, @NonNull final String detailType) {
            mConsumerKey = consumerKey;
            mAccessToken = accessToken;
            mContentType = contentType;
            mDetailType = detailType;
        }
    }
}
