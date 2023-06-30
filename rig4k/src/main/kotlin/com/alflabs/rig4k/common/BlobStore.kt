package com.alflabs.rig4k.common

import com.alflabs.utils.FileOps
import com.alflabs.utils.ILogger
import com.alflabs.utils.StringUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.google.common.base.Charsets
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The blob store caches opaque data for the application.
 *
 *
 * Data is represented by a descriptor, which is treated as an opaque string uniquely describing
 * the data to store and retrieve. For example the caller could have "content-ID" vs "metadata-ID"
 * for a given document. The descriptor is hashed into a SHA1 and this becomes the filename stored
 * in the store.
 *
 *
 * Only 3 data types are supported: String, byte[] and anything serializable via JSON.
 * (optionally the store could support Java serialization or LibUtils Serial, to be added if needed).
 */
@Singleton
class BlobStore @Inject constructor(
    private val blobStoreOptions: BlobStoreOptions,
    private val fileOps: FileOps,
    timing: Timing,
    private val logger: ILogger
) {
    companion object {
        private val TAG = BlobStore::class.java.simpleName
        private const val DEBUG = false
    }

    private val timing = timing.get("BlobStore")

    @Throws(IOException::class)
    fun putBytes(descriptor: String, content: ByteArray) {
        timing.time {
            store(descriptor, "b", content)
        }
    }

    @Throws(IOException::class)
    fun getBytes(descriptor: String): ByteArray? {
        return timing.time {
            retrieve(descriptor, "b")
        }
    }

    @Throws(IOException::class)
    fun putString(descriptor: String, content: String) {
        timing.time {
            store(descriptor, "s", content.toByteArray(Charsets.UTF_8))
        }
    }

    @Throws(IOException::class)
    fun getString(descriptor: String): String? {
        timing.start()
        return try {
            val bytes = retrieve(descriptor, "s") ?: return null
            String(bytes, Charsets.UTF_8)
        } finally {
            timing.end()
        }
    }

    @Throws(IOException::class)
    fun <T> putJson(descriptor: String, content: T) {
        timing.time {
            // // Example version using the com.google.api.client.json.JsonGenerator API.
            // try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            //     JsonGenerator generator = mJsonFactory.createJsonGenerator(baos, Charsets.UTF_8);
            //     generator.enablePrettyPrint();
            //     generator.serialize(content);
            //     generator.flush();
            //     generator.close();
            //     store(descriptor, "j", baos.toByteArray());
            // }

            // Version using the Jackson ObjectMapper API.
            val mapper = ObjectMapper()
            val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
            val bytes: ByteArray = writer.writeValueAsBytes(content)
            store(descriptor, "j", bytes)
        }
    }

    @Throws(IOException::class)
    fun <T> getJson(descriptor: String, clazz: Class<T>?): T? {
        timing.start()
        return try {
            val bytes = retrieve(descriptor, "j") ?: return null
            // Version using the Jackson ObjectMapper API.
            val mapper = ObjectMapper()
            mapper.readValue<T>(bytes, clazz)

            // // Example version using the com.google.api.client.json.JsonParser API.
            // try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            //     JsonParser parser = mJsonFactory.createJsonParser(bais, Charsets.UTF_8);
            //     return parser.parse(clazz);
            // }
        } finally {
            timing.end()
        }
    }

    @Throws(IOException::class)
    private fun store(
        descriptor: String,
        suffix: String,
        content: ByteArray
    ) {
        val key: String = DigestUtils.shaHex(descriptor) + suffix
        val file = File(StringUtils.expandUserHome(blobStoreOptions.blobStoreDir), key)
        fileOps.createParentDirs(file)
        fileOps.writeBytes(content, file)
        if (DEBUG) logger.d(TAG, "BLOB >> Store " + content.size + " bytes to " + file.path)
    }

    @Throws(IOException::class)
    private fun retrieve(descriptor: String, suffix: String): ByteArray? {
        val key: String = DigestUtils.shaHex(descriptor) + suffix
        val file = File(StringUtils.expandUserHome(blobStoreOptions.blobStoreDir), key)
        if (!fileOps.isFile(file)) return null
        val content: ByteArray = fileOps.readBytes(file)
        if (DEBUG) logger.d(TAG, "BLOB << Read  " + content.size + " bytes from " + file.path)
        return content
    }
}
