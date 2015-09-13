package com.emmaguy.videopocket;

import com.emmaguy.videopocket.login.LoginComponent;
import com.emmaguy.videopocket.login.LoginModule;
import com.emmaguy.videopocket.video.VideoComponent;
import com.emmaguy.videopocket.video.VideoModule;

import dagger.Subcomponent;

@ActivityScope @Subcomponent(modules = ActivityModule.class) public interface ActivityComponent {
    LoginComponent plus(LoginModule module);
    VideoComponent plus(VideoModule module);
}
