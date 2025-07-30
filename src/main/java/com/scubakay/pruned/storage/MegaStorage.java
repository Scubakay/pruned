package com.scubakay.pruned.storage;

import java.io.IOException;

public class MegaStorage extends CloudStorage {
    private static MegaStorage instance;
    public static MegaStorage getInstance() {
        if (instance == null) {
            instance = new MegaStorage();
        }
        return instance;
    }

    private String sessionId;
    private String email;
    private String password;

    public void setCredentials(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Override
    public void login() {
        if (email == null || password == null) {
            throw new IllegalStateException("Mega email and password must be set before login.");
        }
        try {
            String passwordHash = hashPassword(password, email);
            String payload = String.format("[{\"a\":\"us\",\"user\":\"%s\",\"uh\":\"%s\"}]", email, passwordHash);
            java.net.URL url = new java.net.URL("https://g.api.mega.co.nz/cs");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            java.io.OutputStream os = conn.getOutputStream();
            os.write(payload.getBytes());
            os.flush();
            os.close();
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                // TODO: Parse session id from response
                this.sessionId = response;
            } else {
                throw new IOException("Mega login failed: " + responseCode);
            }
        } catch (IOException e) {
            throw new RuntimeException("Mega login error", e);
        }
    }

    @Override
    public void uploadFileToSubFolderWithPath(String localFilePath, String mimeType, String worldName, String relativePath) throws IOException {
        // TODO: Implement Mega file upload using a Java SDK or REST API
        throw new UnsupportedOperationException("Mega upload not implemented yet.");
    }

    private String hashPassword(String password, String email) {
        // TODO: Implement Mega's official password hashing algorithm
        // This is a placeholder using SHA-256 for demonstration only
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            String input = email + password;
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
