package com.emmaguy.videopocket;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import java.util.UUID;

import butterknife.ButterKnife;

public abstract class BaseActivity<V extends PresenterView, C extends BaseComponent> extends AppCompatActivity {
    private static final String COMPONENT_KEY = "component_key";
    private String mComponentKey;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mComponentKey = savedInstanceState.getString(COMPONENT_KEY);
        }

        inject(getComponent());
        setContentView(getLayoutId());
        ButterKnife.bind(this);
        onViewCreated(savedInstanceState);
        getPresenter().onViewAttached(getPresenterView());
    }

    @LayoutRes protected abstract int getLayoutId();
    @NonNull protected abstract Presenter<V> getPresenter();
    @NonNull protected abstract V getPresenterView();
    @NonNull protected abstract C createComponent(@NonNull ActivityComponent component);
    protected abstract void inject(@NonNull C component);

    protected void onViewCreated(Bundle savedInstanceState) {

    }

    private C getComponent() {
        final VideoPocketApplication app = VideoPocketApplication.with(this);
        final C component;
        if (mComponentKey == null) {
            mComponentKey = UUID.randomUUID().toString();
            component = createComponent(app.getComponent().plus(new ActivityModule()));
            app.putComponent(mComponentKey, component);
        } else {
            component = app.getComponent(mComponentKey);
            if (component == null) {
                throw new IllegalStateException("Component was not properly stored");
            }
        }

        return component;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(COMPONENT_KEY, mComponentKey);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override protected void onDestroy() {
        getPresenter().onViewDetached();
        ButterKnife.unbind(this);

        if (isFinishing()) {
            VideoPocketApplication.with(this).removeComponent(mComponentKey);
        }

        super.onDestroy();
    }
}
