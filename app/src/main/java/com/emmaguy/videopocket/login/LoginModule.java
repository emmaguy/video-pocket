package com.emmaguy.videopocket.login;

import android.content.res.Resources;

import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.storage.UserStorage;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import retrofit.RestAdapter;
import rx.Scheduler;

@Module public class LoginModule {
    private static final String API_POCKET_URL = "https://getpocket.com";

    @Provides PocketAuthenticationApi providePocketApi() {
        return new RestAdapter.Builder()
                .setEndpoint(API_POCKET_URL)
                .build()
                .create(PocketAuthenticationApi.class);
    }

    @Provides LoginPresenter provideLoginPresenter(PocketAuthenticationApi pocketApi,
            @Named("io") Scheduler ioScheduler, @Named("ui") Scheduler uiScheduler, Resources resources, UserStorage userStorage) {
        String callbackUrl = resources.getString(R.string.callback_url_scheme) + "://" + resources.getString(R.string.callback_url_host);
        return new LoginPresenter(pocketApi, ioScheduler, uiScheduler, userStorage, resources.getString(R.string.pocket_app_id), callbackUrl);
    }
}
