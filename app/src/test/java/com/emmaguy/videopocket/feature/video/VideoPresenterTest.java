package com.emmaguy.videopocket.feature.video;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.emmaguy.videopocket.BasePresenterTest;
import com.emmaguy.videopocket.storage.UserStorage;
import com.emmaguy.videopocket.storage.VideoStorage;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.threeten.bp.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit.RetrofitError;
import retrofit.mime.TypedInput;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class VideoPresenterTest extends BasePresenterTest<VideoPresenter, VideoPresenter.View> {
    private static final int YOUTUBE_REQUEST_LIMIT = 10;

    private static final String DEFAULT_YOUTUBE_ID = "123";
    private static final String DEFAULT_DURATION = "PT2M";
    private static final String DEFAULT_TITLE = "blah";
    private static final String DEFAULT_URL = "http://123";

    private final PublishSubject<Pair<Video, Long>> mArchiveActionSubject = PublishSubject.create();
    private final PublishSubject<SortOrder> mSortOrderChangedSubject = PublishSubject.create();
    private final PublishSubject<String> mSearchQuerySubject = PublishSubject.create();
    private final PublishSubject<Void> mRefreshActionSubject = PublishSubject.create();

    private final TestScheduler mTestIoScheduler = new TestScheduler();

    @Mock private YouTubeParser mYouTubeParser;
    @Mock private VideoStorage mVideoStorage;
    @Mock private UserStorage mUserStorage;
    @Mock private YouTubeApi mYouTubeApi;
    @Mock private PocketApi mPocketApi;
    @Mock private Resources mResources;

    @Captor private ArgumentCaptor<List<Video>> mVideos;
    @Captor private ArgumentCaptor<Map<String, String>> mYouTubeApiCaptor;

    @Override protected VideoPresenter createPresenter() {
        final Observable<Map<String, PocketVideo>> pocketVideosObservable = Observable.just(buildVideosMap());
        when(mPocketApi.videos(any())).thenReturn(pocketVideosObservable);

        final Observable<List<YouTubeVideo>> youTubeVideosObservable = Observable.just(Collections.singletonList(new YouTubeVideo(DEFAULT_YOUTUBE_ID, DEFAULT_DURATION)));
        when(mYouTubeApi.videoData(any(), anyString())).thenReturn(youTubeVideosObservable);

        when(mUserStorage.getSortOrder()).thenReturn(SortOrder.VIDEO_DURATION);
        when(mVideoStorage.getVideos()).thenReturn(new ArrayList<>());

        return new VideoPresenter(mPocketApi, mYouTubeApi, mYouTubeParser, mTestIoScheduler, Schedulers.immediate(), mVideoStorage, mUserStorage, mResources, "", YOUTUBE_REQUEST_LIMIT);
    }

    @Override protected VideoPresenter.View createView() {
        final VideoPresenter.View view = mock(VideoPresenter.View.class);
        when(view.refreshAction()).thenReturn(mRefreshActionSubject);
        when(view.sortOrderChanged()).thenReturn(mSortOrderChangedSubject);
        when(view.archiveAction()).thenReturn(mArchiveActionSubject);
        when(view.searchQueryChanged()).thenReturn(mSearchQuerySubject);
        return view;
    }

    @NonNull private Map<String, PocketVideo> buildVideosMap() {
        final Map<String, PocketVideo> map = new HashMap<>();
        map.put("1", mockPocketVideo(1));
        map.put("2", mockPocketYouTubeVideo(DEFAULT_YOUTUBE_ID, 2));
        map.put("3", mockPocketYouTubeVideo(DEFAULT_YOUTUBE_ID, 3));
        return map;
    }

    @NonNull private PocketVideo mockPocketVideo(final long id) {
        final PocketVideo mock = mock(PocketVideo.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getUrl()).thenReturn(DEFAULT_URL);
        when(mock.getTitle()).thenReturn(DEFAULT_TITLE);
        return mock;
    }

    @NonNull private PocketVideo mockPocketYouTubeVideo(final String youTubeId, final long pocketId) {
        final PocketVideo mock = mockPocketVideo(pocketId);
        final String youTubeUrl = DEFAULT_URL + "?id=" + youTubeId;
        when(mock.getUrl()).thenReturn(youTubeUrl);
        when(mYouTubeParser.getYouTubeId(youTubeUrl)).thenReturn(youTubeId);
        return mock;
    }

    @Test public void onViewAttached_retrieveVideosFromPocket_thenRetrieveDurationsFromYouTube() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView).showLoadingView();
        verify(mPocketApi).videos(any(TypedInput.class));
        verify(mView).hideLoadingView();
        verify(mView).showVideos(mVideos.capture());

        final List<Video> videos = mVideos.getValue();
        assertThat(videos.size(), equalTo(1));
        assertThat(videos.get(0).getTitle(), equalTo(DEFAULT_TITLE));
        assertThat(videos.get(0).getDuration().toMinutes(), equalTo(2l));
        assertThat(videos.get(0).getUrl(), equalTo(DEFAULT_URL));
    }

    @Test public void onViewAttached_afterSuccessfulRetrieval_usesCache() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mVideoStorage).getVideos();
        verify(mPocketApi).videos(any(TypedInput.class));
        verify(mYouTubeApi).videoData(any(), anyString());
        verify(mView).showVideos(any());

        verifyNoMoreInteractions(mPocketApi);
        verifyNoMoreInteractions(mYouTubeApi);

        final Video video = mockVideo(1);
        final List<Video> videos = Collections.singletonList(video);
        when(mVideoStorage.getVideos()).thenReturn(videos);

        verify(mView).hideLoadingView();

        presenterOnViewDetached();
        presenterOnViewAttached();

        verify(mVideoStorage, times(2)).getVideos();
        verify(mView).showVideos(videos);
    }

    @NonNull private Video mockVideo(final long id) {
        final Video video = mock(Video.class);
        when(video.getId()).thenReturn(id);
        when(video.getTitle()).thenReturn(DEFAULT_TITLE);
        when(video.getUrl()).thenReturn(DEFAULT_URL);
        when(video.getDuration()).thenReturn(Duration.parse(DEFAULT_DURATION));
        return video;
    }

    @Test
    public void onViewAttached_retrievingMoreVideosThanYouTubeAPIRequestLimit_batchesCallsToYouTubeApiButResultsAreEmittedTogether() throws Exception {
        final Map<String, PocketVideo> videos = new HashMap<>();

        for (int i = 0; i < 21; i++) {
            final String videoId = "" + (i + 1);
            videos.put(videoId, mockPocketYouTubeVideo(videoId, i));
        }
        final Observable<Map<String, PocketVideo>> videosObservable = Observable.just(videos);
        when(mPocketApi.videos(any(TypedInput.class))).thenReturn(videosObservable);

        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mYouTubeApi, times(3)).videoData(mYouTubeApiCaptor.capture(), anyString());

        final List<Map<String, String>> allValues = mYouTubeApiCaptor.getAllValues();
        assertThat(allValues.get(0).get("id"), equalTo("11,12,13,14,15,16,17,18,19,1"));
        assertThat(allValues.get(1).get("id"), equalTo("2,3,4,5,6,7,8,9,20,10"));
        assertThat(allValues.get(2).get("id"), equalTo("21"));
    }

    @Test
    public void onViewAttached_retrievedVideos_areSortedByDurationAndThoseWithNoDurationAreRemoved() throws Exception {
        final Map<String, PocketVideo> map = new HashMap<>();
        map.put("0", mockPocketVideo(0));
        map.put("1", mockPocketYouTubeVideo("1", 1));
        map.put("2", mockPocketYouTubeVideo("2", 2));
        map.put("3", mockPocketYouTubeVideo("3", 3));
        map.put("4", mockPocketYouTubeVideo("4", 4));
        map.put("5", mockPocketYouTubeVideo("5", 5));
        map.put("6", mockPocketYouTubeVideo("6", 6));

        final Observable<Map<String, PocketVideo>> pocketVideosObservable = Observable.just(map);
        when(mPocketApi.videos(any(TypedInput.class))).thenReturn(pocketVideosObservable);

        final Observable<List<YouTubeVideo>> youTubeVideosObservable = Observable.just(Arrays.asList(
                mockYouTubeVideo("1", Duration.parse("PT2M")),
                mockYouTubeVideo("2", Duration.parse("PT2S")),
                mockYouTubeVideo("3", Duration.parse("PT1S")),
                mockYouTubeVideo("4", Duration.parse("PT1M")),
                mockYouTubeVideo("5", Duration.parse("PT3M")),
                mockYouTubeVideo("6", null)));
        when(mYouTubeApi.videoData(any(), anyString())).thenReturn(youTubeVideosObservable);

        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView).showVideos(mVideos.capture());

        final List<Video> videos = mVideos.getValue();
        assertThat(videos.size(), equalTo(5));
        assertThat(videos.get(0).getDuration(), equalTo(Duration.parse("PT3M")));
        assertThat(videos.get(1).getDuration(), equalTo(Duration.parse("PT2M")));
        assertThat(videos.get(2).getDuration(), equalTo(Duration.parse("PT1M")));
        assertThat(videos.get(3).getDuration(), equalTo(Duration.parse("PT2S")));
        assertThat(videos.get(4).getDuration(), equalTo(Duration.parse("PT1S")));
    }

    @NonNull private YouTubeVideo mockYouTubeVideo(final String id, final Duration duration) {
        final YouTubeVideo mock = mock(YouTubeVideo.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getDuration()).thenReturn(duration);
        return mock;
    }

    @Test
    public void onViewAttached_retrieveVideos_pocketApiThrowsException_hidesLoadingAndShowsError() throws Exception {
        when(mPocketApi.videos(any(TypedInput.class))).thenThrow(RetrofitError.networkError("url", new IOException()));

        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView).hideLoadingView();
        verify(mView).showError();
    }

    @Test
    public void onViewAttached_retrieveYouTubeDurations_youTubeApiThrowsException_hidesLoadingAndShowsError() throws Exception {
        when(mYouTubeApi.videoData(any(), anyString())).thenThrow(RetrofitError.networkError("url", new IOException()));

        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView).hideLoadingView();
        verify(mView).showError();
    }

    @Test public void onViewAttached_retrieveNoVideosFromPocket_hidesLoadingAndShowsError() throws Exception {
        final Observable<Map<String, PocketVideo>> pocketVideosObservable = Observable.just(new HashMap<>());
        when(mPocketApi.videos(any(TypedInput.class))).thenReturn(pocketVideosObservable);

        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView).hideLoadingView();
        verify(mView).showError();

        verifyZeroInteractions(mYouTubeApi);
    }

    @Test public void onViewAttached_retrievesLatestVideos() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView).showLoadingView();
        verify(mView).hideLoadingView();

        verify(mView).showVideos(any());
    }

    @Test public void onRefreshAction_retrievesLatestVideos() throws Exception {
        presenterOnViewAttached();

        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        mRefreshActionSubject.onNext(null);

        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        // times(2) as attaching the view also does 1
        verify(mView, times(2)).showLoadingView();
        verify(mView, times(2)).hideLoadingView();
        verify(mView, times(2)).showVideos(any());
    }

    @Test public void onRefreshAction_whenAnotherRefreshIsAlreadyInProgress_isIgnored() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        mRefreshActionSubject.onNext(null);
        mRefreshActionSubject.onNext(null);

        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        // times(2) as attaching the view also does 1
        verify(mView, times(2)).showLoadingView();
        verify(mView, times(2)).hideLoadingView();
        verify(mView, times(2)).showVideos(any());
    }

    @Test
    public void onViewDetachedReattached_whenRequestInProgress_cancelsRequestAndLetsSubsequentRequestOccur() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        mRefreshActionSubject.onNext(null);

        presenterOnViewDetached();
        presenterOnViewAttached();

        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        // times(3) as attaching the view also does 1 and the refresh starts, but is cancelled when the view is detached
        verify(mView, times(3)).showLoadingView();

        // times(2) as attaching the view also does 1
        verify(mView, times(2)).hideLoadingView();
        verify(mView, times(2)).showVideos(any());
    }

    @Test public void onSortOrderChanged_re_sortsList() throws Exception {
        when(mUserStorage.getSortOrder()).thenReturn(SortOrder.TIME_ADDED_TO_POCKET);

        final List<Video> videos = Arrays.asList(mockVideo(4), mockVideo(2), mockVideo(1), mockVideo(3));
        when(mVideoStorage.getVideos()).thenReturn(videos);

        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        mSortOrderChangedSubject.onNext(SortOrder.TIME_ADDED_TO_POCKET);

        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView, times(3)).showVideos(mVideos.capture());

        final List<Video> sortedVideos = mVideos.getValue();
        assertThat(sortedVideos.size(), equalTo(4));
        assertThat(sortedVideos.get(0).getId(), equalTo(4l));
        assertThat(sortedVideos.get(1).getId(), equalTo(3l));
        assertThat(sortedVideos.get(2).getId(), equalTo(2l));
        assertThat(sortedVideos.get(3).getId(), equalTo(1l));
    }

    @Test public void onSearchQueryChanged_videosAreFilteredCaseInsensitive() throws Exception {
        presenterOnViewAttached();

        final Video videoThatMatches = mockVideo(1);
        when(videoThatMatches.getTitle()).thenReturn("test");

        final Video videoThatDoesntMatch = mockVideo(2);
        when(videoThatDoesntMatch.getTitle()).thenReturn("nonmatch");

        final List<Video> value = Arrays.asList(videoThatDoesntMatch, videoThatMatches);
        when(mVideoStorage.getVideos()).thenReturn(value);

        mSearchQuerySubject.onNext("TE");

        verify(mView).showVideos(mVideos.capture());

        final List<Video> sortedVideos = mVideos.getValue();
        assertThat(sortedVideos.size(), equalTo(1));
        assertThat(sortedVideos.get(0).getTitle(), equalTo("test"));
    }

    @Test public void onArchiveAction_whenArchiveIsSuccessful_archiveItemOnViewAndUpdatesCache() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        final ActionResultResponse successfulResponse = new ActionResultResponse();
        successfulResponse.mStatus = 1;

        when(mPocketApi.archive(any(String.class), any(String.class), any(String.class), any(String.class))).thenReturn(Observable.just(successfulResponse));

        final Video video = mockVideo(1);
        final List<Video> videos = new ArrayList<>();
        videos.add(video);

        when(mVideoStorage.getVideos()).thenReturn(videos);
        when(video.getId()).thenReturn(1l);

        mArchiveActionSubject.onNext(new Pair<>(video, 1l));

        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView).archiveItem(video);
        verify(mVideoStorage).storeVideos(new ArrayList<>());
    }

    @Test public void onArchiveAction_whenArchiveIsUnsuccessful_doesNotArchiveItemOnView() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        final ActionResultResponse successfulResponse = new ActionResultResponse();
        successfulResponse.mStatus = 0;

        when(mPocketApi.archive(any(String.class), any(String.class), any(String.class), any(String.class))).thenReturn(Observable.just(successfulResponse));

        final Video video = mockVideo(1);
        when(mVideoStorage.getVideos()).thenReturn(Collections.singletonList(video));
        when(video.getId()).thenReturn(1l);

        mArchiveActionSubject.onNext(new Pair<>(video, 1l));
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView, never()).archiveItem(video);
        verify(mVideoStorage, never()).storeVideos(new ArrayList<>());
    }

    @Test public void onArchiveAction_whenErrorThrownByNetwork_doesNotArchiveItemOnView() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        final Video video = mockVideo(1);
        when(mVideoStorage.getVideos()).thenReturn(Collections.singletonList(video));
        when(video.getId()).thenReturn(1l);

        when(mPocketApi.archive(any(String.class), any(String.class), any(String.class), any(String.class))).thenThrow(RetrofitError.networkError("url", new IOException()));

        mArchiveActionSubject.onNext(new Pair<>(video, 1l));
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView, never()).archiveItem(video);
        verify(mVideoStorage, never()).storeVideos(new ArrayList<>());
    }
}
