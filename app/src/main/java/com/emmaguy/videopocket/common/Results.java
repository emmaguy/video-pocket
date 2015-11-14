package com.emmaguy.videopocket.common;

import android.support.annotation.NonNull;

import retrofit.Result;

public final class Results {
    private Results() {
    }

    public static boolean isSuccess(@NonNull final Result<?> result) {
        return !result.isError() && result.response().isSuccess();
    }
}