package com.scubakay.pruned.storage;

import java.io.IOException;

public abstract class CloudStorage {
    public abstract void login();
    public abstract void uploadFileToSubFolderWithPath(String localFilePath, String mimeType, String worldName, String relativePath) throws IOException;
}
