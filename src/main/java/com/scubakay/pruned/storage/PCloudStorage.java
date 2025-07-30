package com.scubakay.pruned.storage;

import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.PCloudSdk;
import com.pcloud.sdk.Authenticator;
import com.pcloud.sdk.Authenticators;

public class PCloudStorage {
    private static PCloudStorage instance;
    private ApiClient client;

    public static PCloudStorage getInstance() {
        if (instance == null) {
            instance = new PCloudStorage();
        }
        return instance;
    }

    public boolean login(String username, String password) {
        try {
            Authenticator authenticator = Authenticators.newOAuthAuthenticator("?");
            this.client = PCloudSdk.newClientBuilder()
                    .authenticator(authenticator)
                    .create();
            // Optionally, test the connection here
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public PCloudSdk getClient() {
        return client;
    }
}
