package com.emmaguy.videopocket.common.base;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.emmaguy.videopocket.VideoPocketApplication;
import com.emmaguy.videopocket.feature.ActivityComponent;
import com.emmaguy.videopocket.feature.ActivityModule;

import java.util.UUID;

import butterknife.ButterKnife;

public abstract class BaseActivity<V extends PresenterView, C extends BaseComponent> extends AppCompatActivity {
    private static final String COMPONENT_KEY = "component_key";

    private String componentKey;

    @Override protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            componentKey = savedInstanceState.getString(COMPONENT_KEY);
        }

        inject(getComponent());
        setContentView(getLayoutId());
        ButterKnife.bind(this);
        onViewCreated(savedInstanceState);
        getPresenter().onViewAttached(getPresenterView());
    }

    @LayoutRes protected abstract int getLayoutId();

    @NonNull protected abstract BasePresenter<V> getPresenter();
    @NonNull protected abstract V getPresenterView();
    @NonNull protected abstract C createComponent(@NonNull final ActivityComponent component);

    protected abstract void inject(@NonNull final C component);

    protected void onViewCreated(@Nullable final Bundle savedInstanceState) {

    }

    private C getComponent() {
        final BaseApplication app = BaseApplication.with(this);
        final C component;
        if (componentKey == null) {
            componentKey = UUID.randomUUID().toString();
            component = createComponent(app.getComponent().plus(new ActivityModule()));
            app.putComponent(componentKey, component);
        } else {
            component = app.getComponent(componentKey);
            if (component == null) {
                throw new IllegalStateException("Component was not properly stored");
            }
        }

        return component;
    }

    @Override public void onSaveInstanceState(final Bundle savedInstanceState) {
        savedInstanceState.putString(COMPONENT_KEY, componentKey);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override protected void onDestroy() {
        getPresenter().onViewDetached();
        ButterKnife.unbind(this);

        if (isFinishing()) {
            VideoPocketApplication.with(this).removeComponent(componentKey);
        }

        super.onDestroy();
    }
}
