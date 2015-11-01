package com.emmaguy.videopocket.feature.login;


import com.emmaguy.videopocket.feature.ActivityScope;
import com.emmaguy.videopocket.common.base.BaseComponent;

import dagger.Subcomponent;

@ActivityScope @Subcomponent(modules = LoginModule.class) public interface LoginComponent extends BaseComponent {
    void inject(LoginActivity activity);
}
