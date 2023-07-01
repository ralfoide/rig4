package com.alflabs.rig4k.dl

class ExpGDocEntity {
    private val _metadata: GDocMetadata
    private val _upToDate: Boolean
    private val fetcher: ContentFetcher?
    private val syncToStore: Syncer?
    private var content: ByteArray?
    private var _contentFetched = false

    val metadata: GDocMetadata
        get() = _metadata

    val isToDate: Boolean
        get() = _upToDate

    val isContentFetched: Boolean
        get() = _contentFetched

    constructor(
        metadata: GDocMetadata,
        upToDate: Boolean,
        content: ByteArray?
    ) {
        this._metadata = metadata
        this._upToDate = upToDate
        this.syncToStore = null
        this.content = content
        this.fetcher = null
    }

    constructor(
        metadata: GDocMetadata,
        isUpToDate: Boolean,
        contentFetcher: ContentFetcher?,
        syncToStore: Syncer?
    ) {
        this._metadata = metadata
        this._upToDate = isUpToDate
        this.syncToStore = syncToStore
        this.content = null
        this.fetcher = contentFetcher
    }

    /**
     * Fetches and caches the content.
     *
     * This may fail and return null if there is no associated [ContentFetcher]
     * or [ContentFetcher.fetchContent] failed.
     */
    fun getContent(): ByteArray? {
        if ((content == null || !_contentFetched) && fetcher != null) {
            content = fetcher.fetchContent(this)
            _contentFetched = content != null
        }
        return content
    }

    fun syncToStore() {
        syncToStore?.sync(this)
    }

    fun interface Syncer {
        fun sync(entity: ExpGDocEntity)
    }

    fun interface ContentFetcher {
        fun fetchContent(entity: ExpGDocEntity): ByteArray?
    }
}
