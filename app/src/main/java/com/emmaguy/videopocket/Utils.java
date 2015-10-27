package com.emmaguy.videopocket;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;

import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import timber.log.Timber;

public class Utils {
    @NonNull public static TypedInput buildJson(final Object o) {
        final String json = new Gson().toJson(o);
        try {
            return new TypedByteArray("application/json", json.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Failed to create json");
            throw new RuntimeException("Failed to build json", e);
        }
    }
}
