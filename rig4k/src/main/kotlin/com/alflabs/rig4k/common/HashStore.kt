package com.alflabs.rig4k.common

import com.alflabs.utils.ILogger
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The hash store caches short opaque strings for the application.
 *
 * It is a specialization of the more generic [BlobStore] designed to only store short
 * strings for quick lookup -- typically SHA1 hashes. Anything bigger should go in the
 * [BlobStore] directly.
 *
 * The difference between [HashStore] and [BlobStore] is essentially semantic.
 *
 * There might be some optimization details in the implementation such as caching or loading
 * the store in-memory, which is why clients should not store large values.
 */
@Singleton
class HashStore @Inject constructor(
    private val mLogger: ILogger,
    private val mBlobStore: BlobStore
) {
    companion object {
        private val TAG = HashStore::class.java.simpleName
        private const val DEBUG = false
    }

    private val mCache: MutableMap<String, String> = HashMap()

    @Throws(IOException::class)
    fun putString(descriptor: String, content: String) {
        if (DEBUG) mLogger.d(TAG, "HASH >> write [$descriptor] = $content")
        mBlobStore.putString(descriptor, content)
        mCache[descriptor] = content
    }

    @Throws(IOException::class)
    fun getString(descriptor: String): String? {
        var content = mCache[descriptor]
        if (content == null) {
            content = mBlobStore.getString(descriptor)
            if (DEBUG) mLogger.d(TAG, "HASH << read [$descriptor] = $content")
            if (content != null) {
                mCache[descriptor] = content
            }
        }
        return content
    }
}
