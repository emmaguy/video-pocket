package com.emmaguy.videopocket.feature.video;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.emmaguy.videopocket.BasePresenterTest;
import com.emmaguy.videopocket.storage.UserStorage;
import com.emmaguy.videopocket.storage.VideoStorage;
import com.google.gson.Gson;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.threeten.bp.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.Result;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class VideoPresenterTest extends BasePresenterTest<VideoPresenter, VideoPresenter.View> {
    private static final int YOUTUBE_REQUEST_LIMIT = 10;

    private static final String DEFAULT_YOUTUBE_ID = "youtubeid";

    private static final String DEFAULT_DURATION = "PT2M";
    private static final String DEFAULT_TITLE = "blah";
    private static final String DEFAULT_URL = "http://iamaurl";

    private final PublishSubject<Pair<Video, Long>> archiveActionSubject = PublishSubject.create();
    private final PublishSubject<SortOrder> sortOrderChangedSubject = PublishSubject.create();
    private final PublishSubject<String> searchQuerySubject = PublishSubject.create();
    private final PublishSubject<Void> refreshActionSubject = PublishSubject.create();
    private final PublishSubject<Void> otherSourcesSubject = PublishSubject.create();

    private final TestScheduler testScheduler = new TestScheduler();
    private final Gson gson = new Gson();

    @Mock private YouTubeParser youTubeParser;
    @Mock private VideoStorage videoStorage;
    @Mock private UserStorage userStorage;
    @Mock private YouTubeApi youTubeApi;
    @Mock private PocketApi pocketApi;
    @Mock private Resources resources;

    @Captor private ArgumentCaptor<List<Video>> videosCaptor;
    @Captor private ArgumentCaptor<Map<String, String>> youTubeApiCaptor;
    @Captor private ArgumentCaptor<Map<Integer, Collection<String>>> otherSourcesCaptor;

    @Override protected VideoPresenter createPresenter() {
        final PocketVideoResponse videoResponse = new PocketVideoResponse(buildVideosMap());
        when(pocketApi.videos(any()))
                .thenReturn(Observable.just(Result.response(Response.success(videoResponse))));

        final YouTubeVideoResponse mockYouTubeResponse = new YouTubeVideoResponse(
                Collections.singletonList(buildYouTubeResponse(DEFAULT_DURATION, DEFAULT_YOUTUBE_ID)));
        when(youTubeApi.videoData(any(), anyString()))
                .thenReturn(Observable.just(Result.response(Response.success(mockYouTubeResponse))));

        when(userStorage.getSortOrder()).thenReturn(SortOrder.VIDEO_DURATION);
        when(videoStorage.getVideos()).thenReturn(new ArrayList<>());

        return new VideoPresenter(pocketApi, youTubeApi, youTubeParser, testScheduler, Schedulers.immediate(),
                videoStorage, userStorage, resources, gson, "", YOUTUBE_REQUEST_LIMIT);
    }

    @Override protected VideoPresenter.View createView() {
        final VideoPresenter.View view = mock(VideoPresenter.View.class);
        when(view.refreshAction()).thenReturn(refreshActionSubject);
        when(view.sortOrderChanged()).thenReturn(sortOrderChangedSubject);
        when(view.archiveAction()).thenReturn(archiveActionSubject);
        when(view.searchQueryChanged()).thenReturn(searchQuerySubject);
        when(view.otherSourcesAction()).thenReturn(otherSourcesSubject);
        return view;
    }

    @NonNull private Map<String, PocketVideo> buildVideosMap() {
        final Map<String, PocketVideo> map = new HashMap<>();
        map.put("1", buildPocketVideo(1, DEFAULT_URL));
        map.put("2", buildYouTubeVideo(DEFAULT_YOUTUBE_ID, 2));
        map.put("3", buildYouTubeVideo("youtubeid2", 3));
        return map;
    }

    @NonNull private PocketVideo buildPocketVideo(final long id, @NonNull final String url) {
        return new PocketVideo(id, DEFAULT_TITLE, url);
    }

    @NonNull private PocketVideo buildYouTubeVideo(final String youTubeId, final long pocketId) {
        return buildYouTubeVideo(youTubeId, pocketId, DEFAULT_TITLE);
    }

    @NonNull private PocketVideo buildYouTubeVideo(final String youTubeId, final long pocketId, final String title) {
        final String youTubeUrl =  "http://www.youtube?id=" + youTubeId;
        final PocketVideo pocketVideo = new PocketVideo(pocketId, title, youTubeUrl);
        when(youTubeParser.getYouTubeId(youTubeUrl)).thenReturn(youTubeId);
        return pocketVideo;
    }

    @Test public void onViewAttached_retrieveVideosFromPocket_thenRetrieveDurationsFromYouTube() throws Exception {
        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view).showLoadingView();
        verify(pocketApi).videos(any(RequestBody.class));
        verify(view).hideLoadingView();
        verify(view).showVideos(videosCaptor.capture());

        final List<Video> videos = videosCaptor.getValue();
        assertThat(videos.size(), equalTo(1));
        assertThat(videos.get(0).getTitle(), equalTo(DEFAULT_TITLE));
        assertThat(videos.get(0).getDuration().toMinutes(), equalTo(2l));
        assertThat(videos.get(0).getUrl(), equalTo("http://www.youtube?id=youtubeid"));
    }

    @Test public void onViewAttached_storedOtherSourcesAreDefaultUrl() throws Exception {
        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(userStorage).storeOtherSources(otherSourcesCaptor.capture());

        final Map<Integer, Collection<String>> otherSources = otherSourcesCaptor.getValue();
        assertThat(otherSources.size(), equalTo(1));
        assertThat(otherSources.get(1), equalTo(Collections.singletonList("iamaurl")));
    }

    @Test public void onViewAttached_afterSuccessfulRetrieval_usesCache() throws Exception {
        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(videoStorage).getVideos();
        verify(pocketApi).videos(any(RequestBody.class));
        verify(youTubeApi).videoData(any(), anyString());
        verify(view).showVideos(any());

        verifyNoMoreInteractions(pocketApi);
        verifyNoMoreInteractions(youTubeApi);

        final Video video = mockVideo(1);
        final List<Video> videos = Collections.singletonList(video);
        when(videoStorage.getVideos()).thenReturn(videos);

        verify(view).hideLoadingView();

        presenterOnViewDetached();
        presenterOnViewAttached();

        verify(videoStorage, times(2)).getVideos();
        verify(view).showVideos(videos);
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
            videos.put(videoId, buildYouTubeVideo(videoId, i));
        }
        when(pocketApi.videos(any()))
                .thenReturn(Observable.just(Result.response(Response.success(new PocketVideoResponse(videos)))));

        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(youTubeApi, times(3)).videoData(youTubeApiCaptor.capture(), anyString());

        final List<Map<String, String>> allValues = youTubeApiCaptor.getAllValues();
        assertThat(allValues.get(0).get("id"), equalTo("11,12,13,14,15,16,17,18,19,1"));
        assertThat(allValues.get(1).get("id"), equalTo("2,3,4,5,6,7,8,9,20,10"));
        assertThat(allValues.get(2).get("id"), equalTo("21"));
    }

    @Test
    public void onViewAttached_retrievedVideos_areSortedByDurationAndThoseWithNoDurationAreRemoved() throws Exception {
        final Map<String, PocketVideo> map = new HashMap<>();
        map.put("0", buildPocketVideo(0, DEFAULT_URL));
        map.put("1", buildYouTubeVideo("1", 1));
        map.put("2", buildYouTubeVideo("2", 2));
        map.put("3", buildYouTubeVideo("3", 3));
        map.put("4", buildYouTubeVideo("4", 4));
        map.put("5", buildYouTubeVideo("5", 5));
        map.put("6", buildYouTubeVideo("6", 6));

        when(pocketApi.videos(any())).thenReturn(Observable.just(Result.response(Response.success(new PocketVideoResponse(map)))));

        final YouTubeVideoResponse response = new YouTubeVideoResponse(Arrays.asList(
                buildYouTubeResponse("PT2M", "1"),
                buildYouTubeResponse("PT2S", "2"),
                buildYouTubeResponse("PT1S", "3"),
                buildYouTubeResponse("PT1M", "4"),
                buildYouTubeResponse("PT3M", "5"),
                new YouTubeVideoResponse.YouTubeResponse(null, null, "6")
        ));
        when(youTubeApi.videoData(any(), anyString())).thenReturn(Observable.just(Result.response(Response.success(response))));

        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view).showVideos(videosCaptor.capture());

        final List<Video> videos = videosCaptor.getValue();
        assertThat(videos.size(), equalTo(5));
        assertThat(videos.get(0).getDuration(), equalTo(Duration.parse("PT3M")));
        assertThat(videos.get(1).getDuration(), equalTo(Duration.parse("PT2M")));
        assertThat(videos.get(2).getDuration(), equalTo(Duration.parse("PT1M")));
        assertThat(videos.get(3).getDuration(), equalTo(Duration.parse("PT2S")));
        assertThat(videos.get(4).getDuration(), equalTo(Duration.parse("PT1S")));
    }

    @NonNull
    private YouTubeVideoResponse.YouTubeResponse buildYouTubeResponse(@NonNull final String duration, @NonNull final String id) {
        return new YouTubeVideoResponse.YouTubeResponse(new YouTubeVideoResponse.YouTubeResponse.ContentDetails(duration),
                new YouTubeVideoResponse.YouTubeResponse.Statistics(1), id);
    }

    @Test
    public void onViewAttached_retrieveVideos_pocketApiThrowsException_hidesLoadingAndShowsError() throws Exception {
        when(pocketApi.videos(any(RequestBody.class))).thenReturn(Observable.just(Result.error(new Throwable())));

        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view).hideLoadingView();
        verify(view).showError();
    }

    @Test
    public void onViewAttached_retrieveYouTubeDurations_youTubeApiThrowsException_hidesLoadingAndShowsError() throws Exception {
        when(youTubeApi.videoData(any(), anyString())).thenReturn(Observable.just(Result.error(new Throwable())));

        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view).hideLoadingView();
        verify(view).showError();
    }

    @Test public void onViewAttached_retrieveNoVideosFromPocket_hidesLoadingAndShowsError() throws Exception {
        final PocketVideoResponse videoResponse = new PocketVideoResponse(new HashMap<>());
        when(pocketApi.videos(any()))
                .thenReturn(Observable.just(Result.response(Response.success(videoResponse))));

        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view).hideLoadingView();
        verify(view).showError();

        verifyZeroInteractions(youTubeApi);
    }

    @Test public void onViewAttached_retrievesLatestVideos() throws Exception {
        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view).showLoadingView();
        verify(view).hideLoadingView();

        verify(view).showVideos(any());
    }

    @Test public void onRefreshAction_retrievesLatestVideos() throws Exception {
        presenterOnViewAttached();

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        refreshActionSubject.onNext(null);

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        // times(2) as attaching the view also does 1
        verify(view, times(2)).showLoadingView();
        verify(view, times(2)).hideLoadingView();
        verify(view, times(2)).showVideos(any());
    }

    @Test public void onRefreshAction_whenAnotherRefreshIsAlreadyInProgress_isIgnored() throws Exception {
        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        refreshActionSubject.onNext(null);
        refreshActionSubject.onNext(null);

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        // times(2) as attaching the view also does 1
        verify(view, times(2)).showLoadingView();
        verify(view, times(2)).hideLoadingView();
        verify(view, times(2)).showVideos(any());
    }

    @Test
    public void onViewDetachedReattached_whenRequestInProgress_cancelsRequestAndLetsSubsequentRequestOccur() throws Exception {
        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        refreshActionSubject.onNext(null);

        presenterOnViewDetached();
        presenterOnViewAttached();

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        // times(3) as attaching the view also does 1 and the refresh starts, but is cancelled when the view is detached
        verify(view, times(3)).showLoadingView();

        // times(2) as attaching the view also does 1
        verify(view, times(2)).hideLoadingView();
        verify(view, times(2)).showVideos(any());
    }

    @Test public void onSortOrderChanged_re_sortsList() throws Exception {
        when(userStorage.getSortOrder()).thenReturn(SortOrder.TIME_ADDED_TO_POCKET);

        final List<Video> videos = Arrays.asList(mockVideo(4), mockVideo(2), mockVideo(1), mockVideo(3));
        when(videoStorage.getVideos()).thenReturn(videos);

        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        sortOrderChangedSubject.onNext(SortOrder.TIME_ADDED_TO_POCKET);

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view, times(3)).showVideos(videosCaptor.capture());

        final List<Video> sortedVideos = videosCaptor.getValue();
        assertThat(sortedVideos.size(), equalTo(4));
        assertThat(sortedVideos.get(0).getId(), equalTo(4l));
        assertThat(sortedVideos.get(1).getId(), equalTo(3l));
        assertThat(sortedVideos.get(2).getId(), equalTo(2l));
        assertThat(sortedVideos.get(3).getId(), equalTo(1l));
    }

    @Test public void onSearchQueryChanged_videosAreFilteredCaseInsensitive() throws Exception {
        final Map<String, PocketVideo> map = new HashMap<>();
        map.put("1", buildYouTubeVideo("1", 1, "test"));
        map.put("2", buildYouTubeVideo("2", 2, "nope"));

        when(pocketApi.videos(any())).thenReturn(Observable.just(Result.response(Response.success(new PocketVideoResponse(map)))));

        final YouTubeVideoResponse response = new YouTubeVideoResponse(Arrays.asList(
                buildYouTubeResponse("PT2M", "1"),
                buildYouTubeResponse("PT2S", "2")
        ));
        when(youTubeApi.videoData(any(), anyString())).thenReturn(Observable.just(Result.response(Response.success(response))));

        presenterOnViewAttached();

        searchQuerySubject.onNext("TE");
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view).showVideos(videosCaptor.capture());

        final List<Video> sortedVideos = videosCaptor.getValue();
        assertThat(sortedVideos.size(), equalTo(1));
        assertThat(sortedVideos.get(0).getTitle(), equalTo("test"));
    }

    @Test public void onRefreshAction_whilstSearchQueryIsActive_doesNotResetSearch() throws Exception {
        final Map<String, PocketVideo> map = new HashMap<>();
        map.put("1", buildYouTubeVideo("1", 1, "title"));
        map.put("2", buildYouTubeVideo("2", 2, "nope"));
        map.put("3", buildYouTubeVideo("3", 3, "title"));
        map.put("4", buildYouTubeVideo("4", 4, "nope"));
        map.put("5", buildYouTubeVideo("5", 5, "title"));

        when(pocketApi.videos(any())).thenReturn(Observable.just(Result.response(Response.success(new PocketVideoResponse(map)))));

        final YouTubeVideoResponse response = new YouTubeVideoResponse(Arrays.asList(
                buildYouTubeResponse("PT2M", "1"),
                buildYouTubeResponse("PT2S", "2"),
                buildYouTubeResponse("PT1S", "3"),
                buildYouTubeResponse("PT1M", "4"),
                buildYouTubeResponse("PT3M", "5")
        ));
        when(youTubeApi.videoData(any(), anyString())).thenReturn(Observable.just(Result.response(Response.success(response))));

        presenterOnViewAttached();

        searchQuerySubject.onNext("title");

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        reset(view);

        refreshActionSubject.onNext(null);

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view).showVideos(videosCaptor.capture());

        final List<Video> sortedVideos = videosCaptor.getValue();
        assertThat(sortedVideos.size(), equalTo(3));
        assertThat(sortedVideos.get(0).getTitle(), equalTo("title"));
    }

    @Test public void onArchiveAction_whenArchiveIsSuccessful_archiveItemOnViewAndUpdatesCache() throws Exception {
        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        final ActionResultResponse successfulResponse = new ActionResultResponse();
        successfulResponse.status = 1;

        when(pocketApi.archive(any(String.class), any(String.class), any(String.class)))
                .thenReturn(Observable.just(Result.response(Response.success(successfulResponse))));

        final Video video = mockVideo(1);
        final List<Video> videos = new ArrayList<>();
        videos.add(video);

        when(videoStorage.getVideos()).thenReturn(videos);
        when(video.getId()).thenReturn(1l);

        archiveActionSubject.onNext(new Pair<>(video, 1l));

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view).archiveItem(video);
        verify(videoStorage).storeVideos(new ArrayList<>());
    }

    @Test public void onArchiveAction_whenArchiveIsUnsuccessful_doesNotArchiveItemOnView() throws Exception {
        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        final ActionResultResponse successfulResponse = new ActionResultResponse();
        successfulResponse.status = 0;

        when(pocketApi.archive(any(String.class), any(String.class), any(String.class)))
                .thenReturn(Observable.just(Result.response(Response.success(successfulResponse))));

        final Video video = mockVideo(1);
        when(videoStorage.getVideos()).thenReturn(Collections.singletonList(video));
        when(video.getId()).thenReturn(1l);

        archiveActionSubject.onNext(new Pair<>(video, 1l));
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view, never()).archiveItem(video);
        verify(videoStorage, never()).storeVideos(new ArrayList<>());
    }

    @Test public void onArchiveAction_whenErrorThrownByNetwork_doesNotArchiveItemOnView() throws Exception {
        presenterOnViewAttached();
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        final Video video = mockVideo(1);
        when(videoStorage.getVideos()).thenReturn(Collections.singletonList(video));
        when(video.getId()).thenReturn(1l);

        when(pocketApi.archive(any(String.class), any(String.class), any(String.class)))
                .thenReturn(Observable.just(Result.error(new Throwable())));

        archiveActionSubject.onNext(new Pair<>(video, 1l));
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        verify(view, never()).archiveItem(video);
        verify(videoStorage, never()).storeVideos(new ArrayList<>());
    }
}
