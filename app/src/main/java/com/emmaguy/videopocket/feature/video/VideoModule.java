package com.emmaguy.videopocket.feature.video;

import android.content.res.Resources;

import com.emmaguy.videopocket.feature.ActivityScope;
import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.storage.UserStorage;
import com.emmaguy.videopocket.storage.VideoStorage;
import com.google.gson.Gson;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import rx.Scheduler;

@Module public class VideoModule {
    private static final int YOUTUBE_API_REQUEST_LIMIT = 50;

    @Provides PocketApi providePocketApi(Resources resources) {
        return new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(resources.getString(R.string.pocket_api))
                .build()
                .create(PocketApi.class);
    }

    @Provides YouTubeApi provideYouTubeApi(Resources resources) {
        return new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(resources.getString(R.string.you_tube_api))
                .build()
                .create(YouTubeApi.class);
    }

    @Provides @ActivityScope VideoPresenter provideVideoPresenter(PocketApi pocketApi, YouTubeApi youtubeApi,
                                                                  @Named("io") Scheduler ioScheduler, @Named("ui") Scheduler uiScheduler,
                                                                  Resources resources, UserStorage userStorage, VideoStorage videoStorage,
                                                                  Gson gson) {
        final String youTubeApiKey = resources.getString(R.string.youtube_api_key);
        return new VideoPresenter(pocketApi, youtubeApi, new YouTubeParser(), ioScheduler, uiScheduler, videoStorage, userStorage, resources, gson, youTubeApiKey, YOUTUBE_API_REQUEST_LIMIT);
    }
}
