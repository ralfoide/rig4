package com.alflabs.rig4k.dl

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntityFactory @Inject constructor(
    private val gDocCachedEntityFactory: IGDocCachedEntityFactory,
) {
    fun index(fileId: String) = IndexEntity(gDocCachedEntityFactory, fileId)
    fun article(fileId: String) = ArticleEntity(gDocCachedEntityFactory, fileId)
    fun blog(fileId: String) = BlogEntity(gDocCachedEntityFactory, fileId)
}
