package com.emmaguy.videopocket.storage;

public interface UserStorage {
    String getUsername();
    void storeUsername(String username);

    String getAccessToken();
    void storeAccessToken(String accessToken);

    String getRequestToken();
    void storeRequestToken(String requestTokenCode);
}
