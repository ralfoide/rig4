package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.Timing
import com.alflabs.utils.ILogger
import com.alflabs.utils.StringUtils
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import org.apache.commons.codec.digest.DigestUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class GDocReader @Inject constructor(
    private val mJsonFactory: JsonFactory,
    private val _timing: Timing,
    private val logger: ILogger
) {
    companion object {
        private val TAG = GDocReader::class.java.simpleName
        private const val APPLICATION_NAME = "rig4"
    }

    private val timing = _timing.get("GDocReader")
    private lateinit var mHttpTransport: NetHttpTransport
    private lateinit var mDrive: Drive
    private lateinit var options: GDocReaderOptions


    fun declareFlags() {
//        mFlags.addString(
//            GDOC_ROOT_DIR,
//            "~/.rig42",
//            "Directory where Google Drive API stores credentials files."
//        )
//        mFlags.addString(
//            GDOC_PATH_CLIENT_SECRET_JSON,
//            "\$GDOC_ROOT_DIR/client_secret.json",
//            "Path to load client_secret.json from Google Drive API."
//        )
//        mFlags.addString(
//            GDOC_PATH_DATA_STORE_DIR,
//            "\$GDOC_ROOT_DIR/gdoc_store",
//            "Directory where the Google Drive API stores local credentials."
//        )
    }

    fun init(options: GDocReaderOptions) {
        this.options = options
        timing.start()
        mHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val credential = authorize()
        // set up the global Drive instance
        mDrive = Drive.Builder(mHttpTransport, mJsonFactory, credential)
            .setApplicationName(APPLICATION_NAME)
            .build()
        timing.end()
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private fun authorize(): Credential {
        // load client secrets
        val clientSecrets = getGoogleClientSecrets()
        val dataStoreFactory = getFileDataStoreFactory()

        // We want both metadata *and* file content, read-only scope below is good enough.
        // Tip: the drive auth is in the path of the OAuth2 config/token link so if this needs
        // to be changed then the .credential json file needs to be wiped out.

        // set up authorization code flow
        val flow = GoogleAuthorizationCodeFlow.Builder(
            mHttpTransport,
            mJsonFactory,
            clientSecrets,
            listOf(DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_METADATA_READONLY)
        )
            .setDataStoreFactory(dataStoreFactory)
            .build()

        // authorize
        return AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize("user")
    }

    @Throws(IOException::class)
    private fun getFileDataStoreFactory(): FileDataStoreFactory {
        val path = StringUtils.expandUserHome(
            options.gdocPathDataStoreDir
                .replace("\$GDOC_ROOT_DIR", options.gdocRootDir)
        )
        return try {
            FileDataStoreFactory(File(path))
        } catch (e: IOException) {
            logger.d(TAG, "Error with the gdoc data store directory at $path", e)
            throw e
        }
    }

    @Throws(IOException::class)
    private fun getGoogleClientSecrets(): GoogleClientSecrets {
        val path = StringUtils.expandUserHome(
            options.gdocPathClientSecretJson
                .replace("\$GDOC_ROOT_DIR", options.gdocRootDir)
        )
        return try {
            GoogleClientSecrets.load(mJsonFactory, FileReader(path))
        } catch (e: IOException) {
            logger.d(
                TAG,
                "Enter Client ID and Secret from https://code.google.com/apis/console/?api=drive"
                        + " (nav > API > Credentials > ID > Download) into "
                        + path, e
            )
            throw e
        }
    }

    /**
     * Retrieve the exported content of a file.
     */
    @Throws(IOException::class)
    fun readFileById(fileId: String, mimeType: String): ByteArray {
        // https://developers.google.com/drive/v3/web/manage-downloads
        timing.start()
        return try {
            val baos = ByteArrayOutputStream()
            mDrive.files().export(fileId, mimeType).executeAndDownloadTo(baos)
            baos.toByteArray()
        } finally {
            timing.end()
        }
    }

    /**
     * Retrieve a SHA1 hash that indicates whether the content as changed.
     */
    @Throws(IOException::class)
    fun getMetadataById(fileId: String): GDocMetadata {
        // We need to explicitely tell which fields we want, otherwsie the response
        // contains nothing useful. This is still a hint and some fields might just
        // be missing (e.g. the md5 checksum on a gdoc).
        timing.start()
        return try {
            val get = mDrive.files()[fileId]
                .setFields("md5Checksum,modifiedTime,version,name,exportLinks")
            val gfile = get.execute()
            val version = gfile.version
            val checksum = gfile.md5Checksum
            val dateTime = gfile.modifiedTime
            val exportLinks = gfile.exportLinks
            var hash =
                String.format("v:%s|d:%s|c:%s", version, dateTime, checksum)
            hash = DigestUtils.shaHex(hash)
            GDocMetadata(gfile.name, hash, exportLinks)
        } finally {
            timing.end()
        }
    }

    @Throws(IOException::class)
    fun getDataByUrl(url: URL): InputStream {
        timing.start()
        try {
            var timeoutSeconds = 30
            var retry = 0
            while (true) {
                try {
                    val request = mDrive!!.requestFactory.buildGetRequest(GenericUrl(url))
                    request.setReadTimeout(1000 * timeoutSeconds) // read timeout in milliseconds
                    request.setThrowExceptionOnExecuteError(true)
                    val response = request.execute()
                    return response.content
                } catch (e: SocketTimeoutException) {
                    if (retry > 3) {
                        throw e
                    }
                    logger.d(
                        TAG,
                        "SocketTimeoutException retry: $retry, timeout:$timeoutSeconds seconds, URL:$url"
                    )
                    try {
                        Thread.sleep(1000L * (timeoutSeconds / 2))
                    } catch (ignore: InterruptedException) {
                    }
                    timeoutSeconds *= 2
                    retry++
                }
            }
        } finally {
            timing.end()
        }
    }
}
