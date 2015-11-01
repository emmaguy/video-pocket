package com.emmaguy.videopocket.feature;

import com.emmaguy.videopocket.feature.login.LoginComponent;
import com.emmaguy.videopocket.feature.login.LoginModule;
import com.emmaguy.videopocket.feature.video.VideoComponent;
import com.emmaguy.videopocket.feature.video.VideoModule;

import dagger.Subcomponent;

@ActivityScope @Subcomponent(modules = ActivityModule.class) public interface ActivityComponent {
    LoginComponent plus(LoginModule module);
    VideoComponent plus(VideoModule module);
}
