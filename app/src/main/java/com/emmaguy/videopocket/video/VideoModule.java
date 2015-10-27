package com.emmaguy.videopocket.video;

import android.content.res.Resources;

import com.emmaguy.videopocket.ActivityScope;
import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.storage.UserStorage;
import com.emmaguy.videopocket.storage.VideoStorage;
import com.google.gson.Gson;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import rx.Scheduler;

@Module public class VideoModule {
    private static final int YOUTUBE_API_REQUEST_LIMIT = 50;

    @Provides PocketApi providePocketApi(Resources resources) {
        return new RestAdapter.Builder()
                .setConverter(new VideoConverter(new GsonConverter(new Gson())))
                .setEndpoint(resources.getString(R.string.pocket_api))
                .build()
                .create(PocketApi.class);
    }

    @Provides YouTubeApi provideYouTubeApi(Resources resources) {
        return new RestAdapter.Builder()
                .setConverter(new YouTubeVideoConverter(new GsonConverter(new Gson())))
                .setEndpoint(resources.getString(R.string.you_tube_api))
                .build()
                .create(YouTubeApi.class);
    }

    @Provides @ActivityScope VideoPresenter provideVideoPresenter(PocketApi pocketApi, YouTubeApi youtubeApi,
                                                                  @Named("io") Scheduler ioScheduler, @Named("ui") Scheduler uiScheduler,
                                                                  Resources resources, UserStorage userStorage, VideoStorage videoStorage) {
        final String youTubeApiKey = resources.getString(R.string.youtube_api_key);
        return new VideoPresenter(pocketApi, youtubeApi, new YouTubeParser(), ioScheduler, uiScheduler, videoStorage, userStorage, resources, youTubeApiKey, YOUTUBE_API_REQUEST_LIMIT);
    }
}
