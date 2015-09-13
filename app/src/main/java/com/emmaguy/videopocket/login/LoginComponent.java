package com.emmaguy.videopocket.login;


import com.emmaguy.videopocket.ActivityScope;
import com.emmaguy.videopocket.BaseComponent;

import dagger.Subcomponent;

@ActivityScope @Subcomponent(modules = LoginModule.class) public interface LoginComponent extends BaseComponent {
    void inject(LoginActivity activity);
}
