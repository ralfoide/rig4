package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.BlobStore
import com.alflabs.rig4k.common.HashStore
import com.alflabs.utils.ILogger
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Preconditions
import com.google.common.io.ByteStreams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.net.URL
import javax.inject.Provider

@AssistedFactory
interface IGDocCachedEntityFactory {
    fun create(
        @Assisted("fileId") fileId: String,
        @Assisted("mimeType") mimeType: String,
    ): GDocCachedEntity
}

interface IGDocCachedEntity {
    val fileId: String
    val mimeType: String
    val contentHash: String
    val isAvailable: Boolean
    val metadata: GDocMetadata

    fun hasChanged(targetContentHash: String): Boolean
    fun getContent(): ByteArray
    fun preloadFromGDoc()
    fun preloadFromCache()

    @VisibleForTesting
    fun preloadForTesting(metadata: GDocMetadata, content: ByteArray)
}

class GDocCachedEntity @AssistedInject constructor(
    private val logger: ILogger,
    private val gDocReader: GDocReader,
    private val blobStore: BlobStore,
    private val hashStore: HashStore,
    @Assisted("fileId") override val fileId: String,
    @Assisted("mimeType") override val mimeType: String,
): IGDocCachedEntity {
    companion object {
        private val TAG = GDocCachedEntity::class.java.simpleName
    }

    private val metadataKey = "gdoc-metadata-$fileId"
    private val contentKey = "gdoc-content-$fileId-$mimeType"
    private var _metadata: GDocMetadata? = null
    private var fetcher: Provider<ByteArray>? = null
    private var _content: ByteArray? = null

    override val metadata: GDocMetadata
        get() = _metadata!!

    override val contentHash
        get() = _metadata!!.contentHash

    override val isAvailable
        get() = fetcher != null

    override fun hasChanged(targetContentHash: String): Boolean {
        assert(isAvailable)
        return contentHash == targetContentHash
    }

    override fun getContent(): ByteArray {
        assert(isAvailable)
        if (_content == null) {
            _content = fetcher!!.get()
        }
        return _content!!
    }

    override fun preloadFromGDoc() {
        // Synopsys / Workflow for a cached gdoc entity:
        // - get gdoc metadata once.
        // - compute content hash.
        // - validate cache is valid.
        // - compare content hash with cached content hash, or has never been cached?
        //      - if stale, actually fetch data.
        //      - then store it in cache.
        //      - creates a getter that uses the in-memory/cached content.
        // - creates a getter than fetches content from cache.

        if (_metadata == null && blobStore.hasJson(metadataKey)) {
            _metadata = blobStore.getJson(metadataKey, GDocMetadata::class.java)!!
        }
        val oldMetadata = _metadata

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

            _metadata = newMetadata
            val exportLink = _metadata!!.exportLinks[mimeType]
            val url = URL(exportLink!!)
            gDocReader.getDataByUrl(url)
                .use { inputStream -> _content = ByteStreams.toByteArray(inputStream) }
            Preconditions.checkNotNull<ByteArray>(_content) // fail fast

            blobStore.putJson(metadataKey, _metadata!!)
            blobStore.putBytes(contentKey, _content!!)

            _content!!
        }
    }

    override fun preloadFromCache() {
        if (_metadata == null) {
            _metadata = blobStore.getJson(metadataKey, GDocMetadata::class.java)!!
        }

        assert(blobStore.hasBytes(contentKey))

        fetcher = Provider {
            val content = blobStore.getBytes(contentKey)
            content!!
        }
    }

    @VisibleForTesting
    override fun preloadForTesting(metadata: GDocMetadata, content: ByteArray) {
        if (this._metadata != null) {
            this._metadata = metadata
        }
        fetcher = Provider {
            content
        }
    }
}
