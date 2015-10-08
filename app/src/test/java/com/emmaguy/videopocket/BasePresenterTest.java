package com.emmaguy.videopocket;

import android.support.annotation.CallSuper;

import org.junit.Before;

import static org.mockito.MockitoAnnotations.initMocks;

public abstract class BasePresenterTest<P extends BasePresenter<V>, V extends PresenterView> {
    protected P mPresenter;
    protected V mView;

    @CallSuper @Before public void before() {
        initMocks(this);

        mPresenter = createPresenter();
        mView = createView();
    }

    protected abstract P createPresenter();
    protected abstract V createView();

    protected void presenterOnViewAttached() {
        mPresenter.onViewAttached(mView);
    }

    protected void presenterOnViewDetached() {
        mPresenter.onViewDetached();
    }
}
