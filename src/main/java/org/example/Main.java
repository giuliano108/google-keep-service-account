package org.example;

import com.google.api.client.googleapis.apache.v2.GoogleApacheHttpTransport;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.keep.v1.Keep;
import com.google.api.services.keep.v1.KeepScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

class MyHttpTransportFactory implements HttpTransportFactory {
    public HttpTransport create() {
        try {
            return GoogleApacheHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

public class Main {
    private static final String APPLICATION_NAME = "Google Keep Service Account example";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String USER_TO_IMPERSONATE = "john@doe.mo";
    private static final List<String> KEEP_SCOPES = List.of(KeepScopes.KEEP_READONLY);
    // If authentication fails with `401 Unauthorized`,
    // setting this to true might give you some more info on why that's happening.
    // Check the README.md for details.
    private static final Boolean USE_APACHE_HTTP_TRANSPORT = false;

    private static GoogleCredentials getCredentials() throws IOException {
        InputStream in = Main.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        if (USE_APACHE_HTTP_TRANSPORT) {
            return ServiceAccountCredentials.fromStream(in, new MyHttpTransportFactory()).createScoped(KEEP_SCOPES).createDelegated(USER_TO_IMPERSONATE);
        } else {
            return ServiceAccountCredentials.fromStream(in).createScoped(KEEP_SCOPES).createDelegated(USER_TO_IMPERSONATE);
        }
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        Logger httpLogger = Logger.getLogger(HttpTransport.class.getName());
        httpLogger.setLevel(Level.ALL);
        ConsoleHandler logHandler = new ConsoleHandler();
        logHandler.setLevel(Level.ALL);
        httpLogger.addHandler(logHandler);

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credentials = getCredentials();
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        Keep service =
                new Keep.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                        .setApplicationName(APPLICATION_NAME)
                        .build();

        service.notes().list().execute();
    }
}
