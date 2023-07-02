package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.BlobStore
import com.alflabs.rig4k.common.HashStore
import com.alflabs.utils.ILogger
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Preconditions
import com.google.common.io.ByteStreams
import java.net.URL
import javax.inject.Provider

open class GDocCachedEntity(
    val fileId: String,
    val mimeType: String,
) {
    companion object {
        private val TAG = GDocCachedEntity::class.java.simpleName
    }
    private val metadataKey = "gdoc-metadata-$fileId"
    private val contentKey = "gdoc-content-$fileId-$mimeType"
    private var metadata: GDocMetadata? = null
    private var fetcher: Provider<ByteArray>? = null
    private var _content: ByteArray? = null
    private val contentHash
        get() = metadata!!.contentHash

    val isAvailable
        get() = fetcher != null

    fun hasChanged(targetContentHash: String): Boolean {
        assert(isAvailable)
        return contentHash == targetContentHash
    }

    fun getContent(): ByteArray {
        assert(isAvailable)
        if (_content == null) {
            _content = fetcher!!.get()
        }
        return _content!!
    }

    fun preloadFromGDoc(
        logger: ILogger,
        gDocReader: GDocReader,
        blobStore: BlobStore,
        hashStore: HashStore,
    ) {
        // Synopsys / Workflow for a cached gdoc entity:
        // - get gdoc metadata once.
        // - compute content hash.
        // - validate cache is valid.
        // - compare content hash with cached content hash, or has never been cached?
        //      - if stale, actually fetch data.
        //      - then store it in cache.
        //      - creates a getter that uses the in-memory/cached content.
        // - creates a getter than fetches content from cache.

        if (metadata == null && blobStore.hasJson(metadataKey)) {
            metadata = blobStore.getJson(metadataKey, GDocMetadata::class.java)!!
        }
        val oldMetadata = metadata

        // This can fail with IOException.
        val newMetadata = gDocReader.getMetadataById(fileId)

        if (blobStore.hasBytes(contentKey)
            && oldMetadata != null
            && oldMetadata.contentHash == newMetadata.contentHash) {
            // Fetcher simply returns the blob store data as-is.
            fetcher = Provider {
                val content = blobStore.getBytes(contentKey)
                content!!
            }
            return
        }

        fetcher = Provider {
            logger.d(TAG, ">> Fetching $mimeType: $fileId")

            metadata = newMetadata
            val exportLink = metadata!!.exportLinks[mimeType]
            val url = URL(exportLink!!)
            gDocReader.getDataByUrl(url)
                .use { inputStream -> _content = ByteStreams.toByteArray(inputStream) }
            Preconditions.checkNotNull<ByteArray>(_content) // fail fast

            blobStore.putJson(metadataKey, metadata!!)
            blobStore.putBytes(contentKey, _content!!)

            _content!!
        }
    }

    fun preloadFromCache(blobStore: BlobStore) {
        if (metadata == null) {
            metadata = blobStore.getJson(metadataKey, GDocMetadata::class.java)!!
        }

        assert(blobStore.hasBytes(contentKey))

        fetcher = Provider {
            val content = blobStore.getBytes(contentKey)
            content!!
        }
    }

    @VisibleForTesting
    fun preloadForTesting(metadata: GDocMetadata, content: ByteArray) {
        if (this.metadata != null) {
            this.metadata = metadata
        }
        fetcher = Provider {
            content
        }
    }
}
