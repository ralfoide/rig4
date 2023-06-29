package com.alflabs.rig4k.dl

class GDocEntity {
    private val _metadata: GDocMetadata
    private val _updateToDate: Boolean
    private val mFetcher: ContentFetcher?
    private val mSyncToStore: Syncer?
    private var mContent: ByteArray?
    private var _contentFetched = false

    val metadata: GDocMetadata
        get() = _metadata

    val isUpdateToDate: Boolean
        get() = _updateToDate

    val isContentFetched: Boolean
        get() = _contentFetched

    constructor(metadata: GDocMetadata, updateToDate: Boolean, content: ByteArray?) {
        this._metadata = metadata
        _updateToDate = updateToDate
        mSyncToStore = null
        mContent = content
        mFetcher = null
    }

    constructor(
        metadata: GDocMetadata, updateToDate: Boolean,
        contentFetcher: ContentFetcher?,
        syncToStore: Syncer?
    ) {
        this._metadata = metadata
        _updateToDate = updateToDate
        mSyncToStore = syncToStore
        mContent = null
        mFetcher = contentFetcher
    }

    /**
     * Fetches and caches the content.
     *
     *
     * This may fail and return null if there is no associated [ContentFetcher]
     * or [ContentFetcher.fetchContent] failed.
     */
    fun getContent(): ByteArray? {
        if ((mContent == null || !_contentFetched) && mFetcher != null) {
            mContent = mFetcher.fetchContent(this)
            _contentFetched = mContent != null
        }
        return mContent
    }

    fun syncToStore() {
        mSyncToStore?.sync(this)
    }

    fun interface Syncer {
        fun sync(entity: GDocEntity)
    }

    fun interface ContentFetcher {
        fun fetchContent(entity: GDocEntity): ByteArray?
    }
}
