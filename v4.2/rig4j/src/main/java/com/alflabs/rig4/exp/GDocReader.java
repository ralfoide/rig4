package com.alflabs.rig4.exp;

import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.StringUtils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 *
 */
@Singleton
public class GDocReader {

    private static final String TAG = GDocReader.class.getSimpleName();

    /*
     * API source:
     * https://github.com/google/google-api-java-client-samples/blob/master/drive-cmdline-sample/src/main/java/com/google/api/services/samples/drive/cmdline/DriveSample.java
     */

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String GDOC_PATH_CLIENT_SECRET_JSON = "gdoc-path-client-secret-json";
    private static final String GDOC_PATH_DATA_STORE_DIR = "gdoc-path-data-store-dir";
    private static final String APPLICATION_NAME = "rig4";

    private final Flags mFlags;
    private final ILogger mLogger;
    private NetHttpTransport mHttpTransport;
    private Drive mDrive;


    @Inject
    public GDocReader(Flags flags, ILogger logger) {
        mFlags = flags;
        mLogger = logger;
        flags.addString(GDOC_PATH_CLIENT_SECRET_JSON,
                "~/.rig4/client_secret.json",
                "Path to load client_secret.json from Google Drive API.");
        flags.addString(GDOC_PATH_DATA_STORE_DIR,
                "~/.rig4/gdoc_store",
                "Directory where the Google Drive API stores local credentials.");

    }

    public void init() throws GeneralSecurityException, IOException {
        mHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize();
        // set up the global Drive instance
        mDrive = new Drive.Builder(mHttpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /** Authorizes the installed application to access user's protected data. */
    private Credential authorize() throws IOException {
        // load client secrets
        GoogleClientSecrets clientSecrets = getGoogleClientSecrets();

        FileDataStoreFactory dataStoreFactory = getFileDataStoreFactory();

        // We want both metadata *and* file content, read-only scope below is good enough.
        // Tip: the drive auth is in the path of the OAuth2 config/token link so if this needs
        // to be changed then the .credential json file needs to be wiped out.

        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                mHttpTransport,
                JSON_FACTORY,
                clientSecrets,
                Collections.singleton(DriveScopes.DRIVE_READONLY))
            .setDataStoreFactory(dataStoreFactory)
            .build();

        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    private FileDataStoreFactory getFileDataStoreFactory() throws IOException {
        String path = StringUtils.expandUserHome(mFlags.getString(GDOC_PATH_DATA_STORE_DIR));
        try {
            return new FileDataStoreFactory(new File(path));
        } catch (IOException e) {
            mLogger.d(TAG, "Error with the gdoc data store directory at " + path, e);
            throw e;
        }
    }

    private GoogleClientSecrets getGoogleClientSecrets() throws IOException {
        String path = StringUtils.expandUserHome(mFlags.getString(GDOC_PATH_CLIENT_SECRET_JSON));
        try {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new FileReader(path));
            return clientSecrets;

        } catch (IOException e) {
            mLogger.d(TAG,
                    "Enter Client ID and Secret from https://code.google.com/apis/console/?api=drive"
                    + " into " + path, e);
            throw e;
        }
    }

    /** Retrieve the exported content of a file. */
    public byte[] readFileById(String fileId, String mimeType) throws IOException {
        // https://developers.google.com/drive/v3/web/manage-downloads
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mDrive.files().export(fileId, mimeType).executeAndDownloadTo(baos);
        return baos.toByteArray();
    }

    /** Retrieve the metadata for a file. */
    public void getMetadataById(String fileId) throws IOException {
        Drive.Files.Get get = mDrive.files().get(fileId);
        com.google.api.services.drive.model.File gfile = get.execute();

        // Note: Experimentation shows the only values provided on a gdoc file
        // are id, kind (drive#file), mimeType (application/vnd.google-apps.document) and name.
        // TODO find how to get more.
        String checksum = gfile.getMd5Checksum();
        DateTime dateTime = gfile.getModifiedTime();
        mLogger.d(TAG, fileId + " Checksum: " + checksum);
        mLogger.d(TAG, fileId + " DateTime: " + dateTime);
    }

}
