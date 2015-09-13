package com.emmaguy.videopocket.video;

import com.emmaguy.videopocket.ActivityScope;
import com.emmaguy.videopocket.BaseComponent;

import dagger.Subcomponent;

@ActivityScope @Subcomponent(modules = VideoModule.class) public interface VideoComponent extends BaseComponent {
    void inject(VideoActivity activity);
}

