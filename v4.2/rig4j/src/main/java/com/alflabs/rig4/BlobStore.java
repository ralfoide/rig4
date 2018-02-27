package com.alflabs.rig4;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Charsets;
import org.apache.commons.codec.digest.DigestUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

/**
 * The blob store caches opaque data for the application.
 * <p/>
 * Data is represented by a descriptor, which is treated as an opaque string uniquely describing
 * the data to store and retrieve. For example the caller could have "content-ID" vs "metadata-ID"
 * for a given document. The descriptor is hashed into a SHA1 and this becomes the filename stored
 * in the store.
 * <p/>
 * Only 3 data types are supported: String, byte[] and anything serializable via JSON.
 * (optionally the store could support Java serialization or LibUtils Serial, to be added when needed).
 */
@Singleton
public class BlobStore {
    private static final String BLOB_STORE_DIR = "blob-store-dir";

    private final Flags mFlags;
    private final FileOps mFileOps;
    private final ILogger mLogger;
    private final Timing.TimeAccumulator mTiming;

    @Inject
    public BlobStore(Flags flags, FileOps fileOps, Timing timing, ILogger logger) {
        mFlags = flags;
        mFileOps = fileOps;
        mLogger = logger;
        mTiming = timing.get("BlobStore");
    }

    public void declareFlags() {
        mFlags.addString(BLOB_STORE_DIR,
                "~/.rig42/blob_store",
                "Directory where Rig4j caches local data.");
    }

    public void putBytes(@NonNull String descriptor, @NonNull byte[] content) throws IOException {
        mTiming.start();
        store(descriptor, "b", content);
        mTiming.end();
    }

    @Null
    public byte[] getBytes(@NonNull String descriptor) throws IOException {
        mTiming.start();
        try {
            return retrieve(descriptor, "b");
        } finally {
            mTiming.end();
        }
    }

    public void putString(@NonNull String descriptor, @NonNull String content) throws IOException {
        mTiming.start();
        store(descriptor, "s", content.getBytes(Charsets.UTF_8));
        mTiming.end();
    }

    @Null
    public String getString(@NonNull String descriptor) throws IOException {
        mTiming.start();
        try {
            byte[] bytes = retrieve(descriptor, "s");
            if (bytes == null) return null;
            return new String(bytes, Charsets.UTF_8);
        } finally {
            mTiming.end();
        }
    }

    public <T> void putJson(@NonNull String descriptor, @NonNull T content) throws IOException {
        mTiming.start();
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
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        byte[] bytes = writer.writeValueAsBytes(content);
        store(descriptor, "j", bytes);
        mTiming.end();
    }

    @Null
    public <T> T getJson(@NonNull String descriptor, @NonNull Class<T> clazz) throws IOException {
        mTiming.start();
        try {
            byte[] bytes = retrieve(descriptor, "j");
            if (bytes == null) return null;
            // Version using the Jackson ObjectMapper API.
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bytes, clazz);

            // // Example version using the com.google.api.client.json.JsonParser API.
            // try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            //     JsonParser parser = mJsonFactory.createJsonParser(bais, Charsets.UTF_8);
            //     return parser.parse(clazz);
            // }
        } finally {
            mTiming.end();
        }
    }

    private void store(@NonNull String descriptor, @NonNull String suffix, @NonNull byte[] content) throws IOException {
        String key = DigestUtils.shaHex(descriptor) + suffix;
        File file = new File(StringUtils.expandUserHome(mFlags.getString(BLOB_STORE_DIR)), key);
        mFileOps.createParentDirs(file);
        mFileOps.writeBytes(content, file);
    }

    private byte[] retrieve(@NonNull String descriptor, @NonNull String suffix) throws IOException {
        String key = DigestUtils.shaHex(descriptor) + suffix;
        File file = new File(StringUtils.expandUserHome(mFlags.getString(BLOB_STORE_DIR)), key);
        if (!mFileOps.isFile(file)) return null;
        return mFileOps.readBytes(file);
    }
}
