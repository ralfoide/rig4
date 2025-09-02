package com.alflabs.rig4.gdoc;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.StringUtils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.apache.commons.codec.digest.DigestUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Map;

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

    private static final String GDOC_ROOT_DIR = "gdoc-root-dir";
    private static final String GDOC_PATH_CLIENT_SECRET_JSON = "gdoc-path-client-secret-json";
    private static final String GDOC_PATH_DATA_STORE_DIR = "gdoc-path-data-store-dir";
    private static final String APPLICATION_NAME = "rig4";

    private final JsonFactory mJsonFactory;
    private final Flags mFlags;
    private final ILogger mLogger;
    private final Timing.TimeAccumulator mTiming;
    private NetHttpTransport mHttpTransport;
    private Drive mDrive;


    @Inject
    public GDocReader(JsonFactory jsonFactory, Flags flags, Timing timing, ILogger logger) {
        mJsonFactory = jsonFactory;
        mFlags = flags;
        mLogger = logger;
        mTiming = timing.get("GDocReader");
    }

    public void declareFlags() {
        mFlags.addString(GDOC_ROOT_DIR,
                "~/.rig42",
                "Directory where Google Drive API stores credentials files.");
        mFlags.addString(GDOC_PATH_CLIENT_SECRET_JSON,
                "$GDOC_ROOT_DIR/client_secret.json",
                "Path to load client_secret.json from Google Drive API.");
        mFlags.addString(GDOC_PATH_DATA_STORE_DIR,
                "$GDOC_ROOT_DIR/gdoc_store",
                "Directory where the Google Drive API stores local credentials.");
    }

    public void init() throws GeneralSecurityException, IOException {
        mTiming.start();
        mHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize();
        // set up the global Drive instance
        mDrive = new Drive.Builder(mHttpTransport, mJsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        mTiming.end();
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
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
                mJsonFactory,
                clientSecrets,
                Arrays.asList(DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_METADATA_READONLY))
                .setDataStoreFactory(dataStoreFactory)
                .build();

        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    private FileDataStoreFactory getFileDataStoreFactory() throws IOException {
        String path = StringUtils.expandUserHome(
                mFlags.getString(GDOC_PATH_DATA_STORE_DIR)
                    .replace("$GDOC_ROOT_DIR", mFlags.getString(GDOC_ROOT_DIR)));
        mLogger.d(TAG, "Store located at " + path);
        try {
            return new FileDataStoreFactory(new File(path));
        } catch (IOException e) {
            mLogger.d(TAG, "Error with the gdoc data store directory at " + path, e);
            throw e;
        }
    }

    private GoogleClientSecrets getGoogleClientSecrets() throws IOException {
        String path = StringUtils.expandUserHome(
                mFlags.getString(GDOC_PATH_CLIENT_SECRET_JSON)
                    .replace("$GDOC_ROOT_DIR", mFlags.getString(GDOC_ROOT_DIR)));
        mLogger.d(TAG, "Loading " + path);
        try {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(mJsonFactory, new FileReader(path));
            return clientSecrets;

        } catch (IOException e) {
            mLogger.d(TAG,
                    "Enter Client ID and Secret from https://code.google.com/apis/console/?api=drive"
                    + " (nav > API > Credentials > ID > Download) into "
                    + path, e);
            throw e;
        }
    }

    /**
     * Retrieve the exported content of a file.
     */
    @NonNull
    public byte[] readFileById(String fileId, String mimeType) throws IOException {
        // https://developers.google.com/drive/v3/web/manage-downloads
        mTiming.start();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mDrive.files().export(fileId, mimeType).executeAndDownloadTo(baos);
            return baos.toByteArray();
        } finally {
            mTiming.end();
        }
    }

    /**
     * Retrieve a SHA1 hash that indicates whether the content as changed.
     */
    @NonNull
    public GDocMetadata getMetadataById(String fileId) throws IOException {
        // We need to explicitely tell which fields we want, otherwsie the response
        // contains nothing useful. This is still a hint and some fields might just
        // be missing (e.g. the md5 checksum on a gdoc).
        mTiming.start();
        try {
            Drive.Files.Get get = mDrive.files()
                    .get(fileId)
                    .setFields("md5Checksum,modifiedTime,version,name,exportLinks");
            com.google.api.services.drive.model.File gfile = get.execute();

            Long version = gfile.getVersion();
            String checksum = gfile.getMd5Checksum();
            DateTime dateTime = gfile.getModifiedTime();
            Map<String, String> exportLinks = gfile.getExportLinks();

            String hash = String.format("v:%s|d:%s|c:%s", version, dateTime, checksum);
            hash = DigestUtils.sha256Hex(hash);

            return GDocMetadata.create(gfile.getName(), hash, exportLinks);
        } finally {
            mTiming.end();
        }
    }

    /**
     * Fetches data from a GDrive URL.
     *
     * This handles reties with increasing timeouts and should handle 403 auth access.
     */
    public InputStream getDataByUrl(URL url) throws IOException {
        mTiming.start();
        try {
            int timeoutSeconds = 30;
            int retry = 0;
            while (true) {
                try {
                    HttpRequest request = mDrive.getRequestFactory().buildGetRequest(new GenericUrl(url));
                    request.setReadTimeout(1000 * timeoutSeconds); // read timeout in milliseconds
                    request.setThrowExceptionOnExecuteError(true);
                    HttpResponse response = request.execute();
                    return response.getContent();
                } catch (Exception e) {
                    if (retry > 3) {
                        throw e;
                    }
                    mLogger.d(TAG, e.getClass().getSimpleName() + " retry: " + retry + ", timeout:" + timeoutSeconds + " seconds, URL:" + url.toString());
                    try {
                        Thread.sleep(1000L * (timeoutSeconds / 2));
                    } catch (InterruptedException ignore) {}
                    timeoutSeconds *= 2;
                    retry++;
                }
            }
        } finally {
            mTiming.end();
        }
    }
}
