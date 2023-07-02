package com.alflabs.rig4k.dl

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class IndexEntity @AssistedInject constructor(
    private val gDocCachedEntityFactory: IGDocCachedEntityFactory,
    @Assisted fileId: String,
): IGDocCachedEntity by gDocCachedEntityFactory.create(fileId, "text/plain") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IndexEntity

        return fileId == other.fileId
        // TBD: do we want to equal on content, or just entity properties?
        //                && isAvailable == other.isAvailable
        //                && contentHash == other.contentHash
    }

    override fun hashCode(): Int {
        return fileId.hashCode()
    }

    override fun toString(): String {
        return "IndexEntity(fileId: $fileId)"
    }
}
