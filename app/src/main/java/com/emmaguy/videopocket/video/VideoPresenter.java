package com.emmaguy.videopocket.video;

import android.support.annotation.NonNull;

import com.emmaguy.videopocket.Presenter;
import com.emmaguy.videopocket.PresenterView;
import com.emmaguy.videopocket.StringUtils;
import com.emmaguy.videopocket.storage.VideoStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit.mime.TypedInput;
import rx.Observable;
import rx.Scheduler;
import timber.log.Timber;

class VideoPresenter extends Presenter<VideoPresenter.View> {
    private final YouTubeApi mYouTubeApi;
    private final PocketApi mPocketApi;

    private final YouTubeParser mYouTubeParser;

    private final Scheduler mIoScheduler;
    private final Scheduler mUiScheduler;

    private final VideoStorage mVideoStorage;
    private final TypedInput mRequestTypedInput;

    private final String mYouTubeApiKey;
    private final int mYouTubeRequestLimit;

    private boolean mIsRetrievingInProgress = false;

    VideoPresenter(@NonNull final PocketApi pocketApi, @NonNull final YouTubeApi youTubeApi, @NonNull final YouTubeParser youTubeParser, @NonNull final Scheduler ioScheduler,
                   @NonNull final Scheduler uiScheduler, @NonNull final VideoStorage videoStorage, @NonNull final TypedInput requestTypedInput, @NonNull final String youTubeApiKey, final int youTubeRequestLimit) {
        mPocketApi = pocketApi;
        mYouTubeApi = youTubeApi;
        mYouTubeParser = youTubeParser;
        mIoScheduler = ioScheduler;
        mUiScheduler = uiScheduler;
        mVideoStorage = videoStorage;
        mRequestTypedInput = requestTypedInput;
        mYouTubeApiKey = youTubeApiKey;
        mYouTubeRequestLimit = youTubeRequestLimit;
    }

    @Override
    public void onViewAttached(@NonNull final View view) {
        super.onViewAttached(view);

        final List<Video> cachedVideos = mVideoStorage.getVideos();
        if (!cachedVideos.isEmpty()) {
            view.showVideos(cachedVideos);
        }

        final Observable<List<Video>> videoObservable = Observable.just(mRequestTypedInput)
                .observeOn(mUiScheduler)
                .doOnNext(typedInput -> view.showLoadingView())
                .observeOn(mIoScheduler)
                .flatMap(typedInput -> Observable.defer(() -> mPocketApi.videos(typedInput).map(map -> new ArrayList<>(map.values())))
                        .onErrorResumeNext(throwable -> Observable.just(new ArrayList<>()))
                        .observeOn(mUiScheduler)
                        .map(videos -> validate(videos, view))
                        .filter(videos -> videos != null))
                .flatMap(pocketVideos -> filterForYouTubeVideosAndRetrieveDurations(view, pocketVideos))
                .toSortedList(this::sortVideosByDuration)
                .doOnNext(mVideoStorage::storeVideos)
                .observeOn(mUiScheduler)
                .doOnNext(aVoid -> view.hideLoadingView());

        unsubscribeOnViewDetach(view.refreshActionObservable().startWith(Observable.just(null))
                .filter(av -> !mIsRetrievingInProgress)
                .doOnNext(av -> mIsRetrievingInProgress = true)
                .flatMap(aVoid -> videoObservable)
                .doOnNext(av -> mIsRetrievingInProgress = false)
                .observeOn(mUiScheduler)
                .subscribe(view::showVideos, throwable -> Timber.e(throwable, "Failed to get videos")));
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
                .map(youTubeVideos -> validate(youTubeVideos, view))
                .observeOn(mIoScheduler)
                .flatMap(Observable::from)
                .filter(youTubeVideo -> youTubeVideo.getDuration() != null)
                .map(youTubeVideo -> {
                    for (PocketVideo pocketVideo : pocketVideos) {
                        if (pocketVideo.getUrl().contains(youTubeVideo.getId())) {
                            return new Video(pocketVideo.getTitle(), pocketVideo.getUrl(), youTubeVideo.getDuration());
                        }
                    }
                    return null;
                }).filter(video -> video != null);
    }

    @NonNull private static <T> List<T> validate(@NonNull final List<T> videos, final View view) {
        if (videos.isEmpty()) {
            view.showError();
        }

        return videos;
    }

    private int sortVideosByDuration(@NonNull final Video video, @NonNull final Video video2) {
        if (video.getDuration().getSeconds() > video2.getDuration().getSeconds()) {
            return -1;
        } else if (video.getDuration().getSeconds() < video2.getDuration().getSeconds()) {
            return 1;
        }
        return 0;
    }

    public interface View extends PresenterView {
        @NonNull Observable<Void> refreshActionObservable();

        void showVideos(final @NonNull List<Video> videos);

        void showLoadingView();
        void hideLoadingView();

        void showError();
        void hideError();
    }
}
