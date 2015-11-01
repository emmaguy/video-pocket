package com.emmaguy.videopocket.storage;

import com.emmaguy.videopocket.feature.video.SortOrder;

public interface UserStorage {
    String getUsername();
    void storeUsername(final String username);

    String getAccessToken();
    void storeAccessToken(String accessToken);

    String getRequestToken();
    void storeRequestToken(String requestTokenCode);

    SortOrder getSortOrder();
    void setSortOrder(final SortOrder sortOrder);
}
