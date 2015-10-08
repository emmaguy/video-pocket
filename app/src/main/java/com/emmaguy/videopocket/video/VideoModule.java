package com.emmaguy.videopocket.video;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.emmaguy.videopocket.ActivityScope;
import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.storage.UserStorage;
import com.emmaguy.videopocket.storage.VideoStorage;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.UnsupportedEncodingException;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import rx.Scheduler;
import timber.log.Timber;

@Module public class VideoModule {
    private static final int YOUTUBE_API_REQUEST_LIMIT = 50;

    private static final String API_YOUTUBE_URL = "https://www.googleapis.com";
    private static final String API_POCKET_URL = "https://getpocket.com";

    @Provides PocketApi providePocketApi() {
        return new RestAdapter.Builder()
                .setConverter(new VideoConverter(new GsonConverter(new Gson())))
                .setEndpoint(API_POCKET_URL)
                .build()
                .create(PocketApi.class);
    }

    @Provides YouTubeApi provideYouTubeApi() {
        return new RestAdapter.Builder()
                .setConverter(new YouTubeVideoConverter(new GsonConverter(new Gson())))
                .setEndpoint(API_YOUTUBE_URL)
                .build()
                .create(YouTubeApi.class);
    }

    @Provides @ActivityScope VideoPresenter provideVideoPresenter(PocketApi pocketApi, YouTubeApi youtubeApi,
            @Named("io") Scheduler ioScheduler, @Named("ui") Scheduler uiScheduler, Resources resources, UserStorage userStorage, VideoStorage videoStorage) {
        final TypedInput typedInput = buildJson(resources.getString(R.string.pocket_app_id), userStorage);
        final String youTubeApiKey = resources.getString(R.string.youtube_api_key);
        return new VideoPresenter(pocketApi, youtubeApi, new YouTubeParser(), ioScheduler, uiScheduler, videoStorage, userStorage, typedInput, youTubeApiKey, YOUTUBE_API_REQUEST_LIMIT);
    }

    @NonNull private TypedInput buildJson(@NonNull final String consumerKey, @NonNull final UserStorage userStorage) {
        final String json = new Gson().toJson(new RequestHolder(consumerKey, userStorage.getAccessToken(), "video", "simple"));
        try {
            return new TypedByteArray("application/json", json.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Failed to create json");
            throw new RuntimeException("Failed to create TypedInput for Pocket api request");
        }
    }

    static class RequestHolder {
        @SerializedName("consumer_key") final String mConsumerKey;
        @SerializedName("access_token") final String mAccessType;
        @SerializedName("contentType") final String mContentType;
        @SerializedName("detailType") final String mDetailType;

        RequestHolder(String consumerKey, String accessType, String contentType, String detailType) {
            mConsumerKey = consumerKey;
            mAccessType = accessType;
            mContentType = contentType;
            mDetailType = detailType;
        }
    }
}
