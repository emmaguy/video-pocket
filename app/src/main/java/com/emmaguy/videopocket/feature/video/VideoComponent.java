package com.emmaguy.videopocket.feature.video;

import com.emmaguy.videopocket.feature.ActivityScope;
import com.emmaguy.videopocket.common.base.BaseComponent;

import dagger.Subcomponent;

@ActivityScope @Subcomponent(modules = VideoModule.class) public interface VideoComponent extends BaseComponent {
    void inject(VideoActivity activity);
}

