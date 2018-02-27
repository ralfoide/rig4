package com.alflabs.rig4;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The hash store caches short opaque strings for the application.
 * <p/>
 * It is a specialization of the more generic {@link BlobStore} designed to only store short
 * strings for quick lookup -- typically SHA1 hashes. Anything bigger should go in the
 * {@link BlobStore} directly.
 * <p/>
 * The difference between {@link HashStore} and {@link BlobStore} is essentially semantic.
 * <p/>
 * There might be some optimization details in the implementation such as caching or loading
 * the store in-memory, which is why clients should not store large values.
 */
@Singleton
public class HashStore {
    private final Map<String, String> mCache = new HashMap<>();
    private final BlobStore mBlobStore;

    @Inject
    public HashStore(BlobStore blobStore) {
        mBlobStore = blobStore;
    }

    public void putString(@NonNull String descriptor, @NonNull String content) throws IOException {
        mBlobStore.putString(descriptor, content);
        mCache.put(descriptor, content);
    }

    @Null
    public String getString(@NonNull String descriptor) throws IOException {
        String content = mCache.get(descriptor);
        if (content == null) {
            content = mBlobStore.getString(descriptor);
            if (content != null) {
                mCache.put(descriptor, content);
            }
        }
        return content;
    }
}
