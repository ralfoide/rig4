package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.BlobStore
import com.alflabs.rig4k.common.HashStore
import com.alflabs.rig4k.common.Timing
import com.alflabs.utils.FileOps
import com.alflabs.utils.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GDocHelper @Inject constructor(
    private val logger: ILogger,
    private val fileOps: FileOps,
    private val timing: Timing,
    private val gDocReader: GDocReader,
    private val blobStore: BlobStore,
    private val hashStore: HashStore,
) {
    companion object {
        private val TAG = GDocHelper::class.java.simpleName
    }

    fun preload(entity: IGDocCachedEntity) {
        entity.preloadFromGDoc()
    }
}
