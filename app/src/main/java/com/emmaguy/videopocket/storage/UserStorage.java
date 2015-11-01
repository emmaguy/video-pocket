package com.emmaguy.videopocket.storage;

import android.support.annotation.NonNull;

import com.emmaguy.videopocket.feature.video.SortOrder;

public interface UserStorage {
    @NonNull String getUsername();
    void storeUsername(final String username);

    @NonNull String getAccessToken();
    void storeAccessToken(final String accessToken);

    @NonNull String getRequestToken();
    void storeRequestToken(final String requestTokenCode);

    SortOrder getSortOrder();
    void setSortOrder(final SortOrder sortOrder);
}
