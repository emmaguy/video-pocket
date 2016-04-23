package com.emmaguy.videopocket.feature.login;

import android.content.res.Resources;

import com.emmaguy.videopocket.R;
import com.emmaguy.videopocket.storage.UserStorage;
import com.google.gson.Gson;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Scheduler;

@Module public class LoginModule {
    @Provides PocketAuthenticationApi providePocketApi(Resources resources) {
        return new Retrofit.Builder()
                .baseUrl(resources.getString(R.string.pocket_api))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PocketAuthenticationApi.class);
    }

    @Provides LoginPresenter provideLoginPresenter(PocketAuthenticationApi pocketApi,
            @Named("io") Scheduler ioScheduler, @Named("ui") Scheduler uiScheduler, Resources resources, UserStorage userStorage, Gson gson) {
        final String callbackUrl = resources.getString(R.string.callback_url_scheme) + "://" + resources.getString(R.string.callback_url_host);
        return new LoginPresenter(pocketApi, ioScheduler, uiScheduler, userStorage, resources.getString(R.string.pocket_app_id), callbackUrl, gson);
    }
}
