package com.emmaguy.videopocket.video;

import android.support.annotation.NonNull;

import com.emmaguy.videopocket.BasePresenterTest;
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

    @Mock private YouTubeParser mYouTubeParser;
    @Mock private VideoStorage mVideoStorage;
    @Mock private YouTubeApi mYouTubeApi;
    @Mock private TypedInput mTypedInput;
    @Mock private PocketApi mPocketApi;

    @Captor private ArgumentCaptor<List<Video>> mVideos;
    @Captor private ArgumentCaptor<Map<String, String>> mYouTubeApiCaptor;

    private final PublishSubject<Void> mOnRefreshActionSubject = PublishSubject.create();
    private final TestScheduler mTestIoScheduler = new TestScheduler();

    @Override protected VideoPresenter createPresenter() {
        final Observable<Map<String, PocketVideo>> pocketVideosObservable = Observable.just(buildVideosMap());
        when(mPocketApi.videos(mTypedInput)).thenReturn(pocketVideosObservable);

        final Observable<List<YouTubeVideo>> youTubeVideosObservable = Observable.just(Collections.singletonList(new YouTubeVideo(DEFAULT_YOUTUBE_ID, DEFAULT_DURATION)));
        when(mYouTubeApi.videoData(any(), anyString())).thenReturn(youTubeVideosObservable);

        when(mVideoStorage.getVideos()).thenReturn(new ArrayList<>());

        return new VideoPresenter(mPocketApi, mYouTubeApi, mYouTubeParser, mTestIoScheduler, Schedulers.immediate(), mVideoStorage, mTypedInput, "", YOUTUBE_REQUEST_LIMIT);
    }

    @NonNull private Map<String, PocketVideo> buildVideosMap() {
        final Map<String, PocketVideo> map = new HashMap<>();
        map.put("1", mockPocketVideo());
        map.put("2", mockPocketYouTubeVideo(DEFAULT_YOUTUBE_ID));
        map.put("3", mockPocketYouTubeVideo(DEFAULT_YOUTUBE_ID));
        return map;
    }

    @NonNull private PocketVideo mockPocketVideo() {
        final PocketVideo mock = mock(PocketVideo.class);
        when(mock.getUrl()).thenReturn(DEFAULT_URL);
        when(mock.getTitle()).thenReturn(DEFAULT_TITLE);
        return mock;
    }

    @NonNull private PocketVideo mockPocketYouTubeVideo(final String id) {
        final PocketVideo mock = mock(PocketVideo.class);
        final String youTubeId = DEFAULT_URL + "?id=" + id;
        when(mock.getUrl()).thenReturn(youTubeId);
        when(mYouTubeParser.getYouTubeId(youTubeId)).thenReturn(id);
        return mock;
    }

    @NonNull private YouTubeVideo mockYouTubeVideo(final String id, final Duration duration) {
        final YouTubeVideo mock = mock(YouTubeVideo.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getDuration()).thenReturn(duration);
        return mock;
    }

    @Override protected VideoPresenter.View createView() {
        final VideoPresenter.View view = mock(VideoPresenter.View.class);
        when(view.refreshActionObservable()).thenReturn(mOnRefreshActionSubject);
        return view;
    }

    @Test public void onViewAttached_retrieveVideosFromPocket_thenRetrieveDurationsFromYouTube() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(mView).showLoadingView();
        verify(mPocketApi).videos(mTypedInput);
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
        verify(mPocketApi).videos(mTypedInput);
        verify(mYouTubeApi).videoData(any(), anyString());
        verify(mView).showVideos(any());

        verifyNoMoreInteractions(mPocketApi);
        verifyNoMoreInteractions(mYouTubeApi);

        final List<Video> videos = Collections.singletonList(new Video(DEFAULT_TITLE, DEFAULT_URL, Duration.parse(DEFAULT_DURATION)));
        when(mVideoStorage.getVideos()).thenReturn(videos);

        verify(mView).hideLoadingView();

        presenterOnViewDetached();
        presenterOnViewAttached();

        verify(mVideoStorage, times(2)).getVideos();
        verify(mView).showVideos(videos);
    }

    @Test
    public void onViewAttached_retrievingMoreVideosThanYouTubeAPIRequestLimit_batchesCallsToYouTubeApiButResultsAreEmittedTogether() throws Exception {
        final Map<String, PocketVideo> videos = new HashMap<>();

        for (int i = 0; i < 21; i++) {
            final String videoId = "" + (i + 1);
            videos.put(videoId, mockPocketYouTubeVideo(videoId));
        }
        final Observable<Map<String, PocketVideo>> videosObservable = Observable.just(videos);
        when(mPocketApi.videos(mTypedInput)).thenReturn(videosObservable);

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
        map.put("0", mockPocketVideo());
        map.put("1", mockPocketYouTubeVideo("1"));
        map.put("2", mockPocketYouTubeVideo("2"));
        map.put("3", mockPocketYouTubeVideo("3"));
        map.put("4", mockPocketYouTubeVideo("4"));
        map.put("5", mockPocketYouTubeVideo("5"));
        map.put("6", mockPocketYouTubeVideo("6"));

        final Observable<Map<String, PocketVideo>> pocketVideosObservable = Observable.just(map);
        when(mPocketApi.videos(mTypedInput)).thenReturn(pocketVideosObservable);

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

    @Test
    public void onViewAttached_retrieveVideos_pocketApiThrowsException_hidesLoadingAndShowsError() throws Exception {
        when(mPocketApi.videos(mTypedInput)).thenThrow(RetrofitError.networkError("url", new IOException()));

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
        when(mPocketApi.videos(mTypedInput)).thenReturn(pocketVideosObservable);

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

        performOnRefreshAction();

        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        // times(2) as attaching the view also does 1
        verify(mView, times(2)).showLoadingView();
        verify(mView, times(2)).hideLoadingView();
        verify(mView, times(2)).showVideos(any());
    }

    @Test public void onRefreshAction_whenAnotherRefreshIsAlreadyInProgress_isIgnored() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        performOnRefreshAction();
        performOnRefreshAction();

        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        // times(2) as attaching the view also does 1
        verify(mView, times(2)).showLoadingView();
        verify(mView, times(2)).hideLoadingView();
        verify(mView, times(2)).showVideos(any());
    }

    @Test public void onViewDetachedReattached_whenRequestInProgress_cancelsRequestAndLetsSubsequentRequestOccur() throws Exception {
        presenterOnViewAttached();
        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        performOnRefreshAction();

        presenterOnViewDetached();
        presenterOnViewAttached();

        mTestIoScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        // times(3) as attaching the view also does 1 and the refresh starts, but is cancelled when the view is detached
        verify(mView, times(3)).showLoadingView();

        // times(2) as attaching the view also does 1
        verify(mView, times(2)).hideLoadingView();
        verify(mView, times(2)).showVideos(any());
    }

    private void performOnRefreshAction() {
        mOnRefreshActionSubject.onNext(null);
    }
}
