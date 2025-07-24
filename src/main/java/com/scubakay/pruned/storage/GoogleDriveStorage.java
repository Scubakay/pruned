package com.scubakay.pruned.storage;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.DriveScopes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class GoogleDriveStorage {
    private static final String APPLICATION_NAME = "Pruned";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "config/pruned/credentials.json";

    private static final List<String> SCOPES =
            Collections.singletonList(DriveScopes.DRIVE_FILE);

    public static void login() throws IOException {
        getCredentials(new NetHttpTransport());
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets from config folder in run directory.
        java.io.File credentialsFile = new java.io.File(CREDENTIALS_FILE_PATH);
        if (!credentialsFile.exists()) {
            throw new FileNotFoundException("File not found: " + credentialsFile.getAbsolutePath());
        }
        InputStream in = new java.io.FileInputStream(credentialsFile);
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        //returns an authorized Credential object.
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Finds or creates a folder named "pruned" in Google Drive and returns its ID.
     */
    private static String getOrCreatePrunedFolderId(Drive service) throws IOException {
        // Search for folder named "pruned"
        String query = "mimeType='application/vnd.google-apps.folder' and name='pruned' and trashed=false";
        List<File> files = service.files().list()
                .setQ(query)
                .setSpaces("drive") // Ensure search is in My Drive
                .setFields("files(id, name)")
                .execute()
                .getFiles();

        if (files != null && !files.isEmpty()) {
            return files.getFirst().getId();
        }

        // Create folder if not found
        File folderMetadata = new File();
        folderMetadata.setName("pruned");
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        File folder = service.files().create(folderMetadata)
                .setFields("id")
                .execute();
        return folder.getId();
    }

    /**
     * Finds or creates a folder with the given name under the specified parent folder in Google Drive.
     */
    private static String getOrCreateSubFolderId(Drive service, String parentId, String subFolderName) throws IOException {
        String query = String.format(
            "mimeType='application/vnd.google-apps.folder' and name='%s' and '%s' in parents and trashed=false",
            subFolderName, parentId
        );
        List<File> files = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
                .getFiles();

        if (files != null && !files.isEmpty()) {
            return files.getFirst().getId();
        }

        File folderMetadata = new File();
        folderMetadata.setName(subFolderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(parentId));
        File folder = service.files().create(folderMetadata)
                .setFields("id")
                .execute();
        return folder.getId();
    }

    /**
     * Finds a file by name in a specific parent folder.
     */
    private static File findFileInFolder(Drive service, String parentId, String fileName) throws IOException {
        String query = String.format(
            "name='%s' and '%s' in parents and trashed=false",
            fileName, parentId
        );
        List<File> files = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, parents)")
                .execute()
                .getFiles();
        if (files != null && !files.isEmpty()) {
            return files.getFirst();
        }
        return null;
    }

    /**
     * Uploads a specific file to Google Drive in the "pruned" folder, preserving subdirectory (region/entities).
     * Overwrites existing file if one with the same name exists in the target folder.
     */
    public static String uploadFileToSubFolder(String localFilePath, String mimeType, String subFolderName) throws IOException {
        final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
        Credential credential = getCredentials(HTTP_TRANSPORT);

        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        String prunedFolderId = getOrCreatePrunedFolderId(service);
        String subFolderId = getOrCreateSubFolderId(service, prunedFolderId, subFolderName);

        java.io.File filePath = new java.io.File(localFilePath);
        String fileName = filePath.getName();

        FileContent mediaContent = new FileContent(mimeType, filePath);

        File existingFile = findFileInFolder(service, subFolderId, fileName);
        File uploadedFile;
        if (existingFile != null) {
            // Overwrite existing file
            uploadedFile = service.files().update(existingFile.getId(), null, mediaContent)
                    .setFields("id, parents")
                    .execute();
        } else {
            // Create new file
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(subFolderId));
            uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
        }

        if (uploadedFile.getParents() == null || !uploadedFile.getParents().contains(subFolderId)) {
            throw new IOException("File was not uploaded to the '" + subFolderName + "' folder.");
        }

        return uploadedFile.getId();
    }

    /**
     * Uploads a file to Google Drive under the "pruned/worldName/relativePath" folder structure.
     * Overwrites existing file if one with the same name exists in the target folder.
     */
    public static String uploadFileToSubFolderWithPath(String localFilePath, String mimeType, String worldName, String relativePath) throws IOException {
        final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
        Credential credential = getCredentials(HTTP_TRANSPORT);

        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        String prunedFolderId = getOrCreatePrunedFolderId(service);
        String parentId = getOrCreateSubFolderId(service, prunedFolderId, worldName);

        String[] parts = relativePath.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            parentId = getOrCreateSubFolderId(service, parentId, parts[i]);
        }

        String fileName = parts.length > 0 ? parts[parts.length - 1] : new java.io.File(localFilePath).getName();

        java.io.File filePath = new java.io.File(localFilePath);
        FileContent mediaContent = new FileContent(mimeType, filePath);

        File existingFile = findFileInFolder(service, parentId, fileName);
        File uploadedFile;
        if (existingFile != null) {
            // Overwrite existing file
            uploadedFile = service.files().update(existingFile.getId(), null, mediaContent)
                    .setFields("id, parents")
                    .execute();
        } else {
            // Create new file
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(parentId));
            uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
        }

        if (uploadedFile.getParents() == null || !uploadedFile.getParents().contains(parentId)) {
            throw new IOException("File was not uploaded to the correct folder.");
        }

        return uploadedFile.getId();
    }
}
