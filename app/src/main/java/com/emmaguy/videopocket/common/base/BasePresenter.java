package com.emmaguy.videopocket.common.base;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class BasePresenter<V extends PresenterView> {
    private CompositeSubscription mSubscriptions;
    private V mView;

    @CallSuper public void onViewAttached(@NonNull final V view) {
        if (mView != null) {
            throw new IllegalStateException("View " + mView + " is already attached. Cannot attach " + view);
        }
        mView = view;
    }

    @CallSuper public void onViewDetached() {
        if (mView == null) {
            throw new IllegalStateException("View is already detached");
        }
        mView = null;

        if (mSubscriptions != null) {
            mSubscriptions.unsubscribe();
            mSubscriptions = null;
        }
    }

    @CallSuper protected void unsubscribeOnViewDetach(@NonNull final Subscription subscription) {
        if (mSubscriptions == null) {
            mSubscriptions = new CompositeSubscription();
        }
        mSubscriptions.add(subscription);
    }
}
