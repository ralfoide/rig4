package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.Timing
import com.alflabs.rig4k.site.Site
import com.alflabs.rig4k.site.SiteOptions
import com.alflabs.utils.ILogger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreloadCommand @Inject constructor(
    private val logger: ILogger,
    private val timing: Timing,
    private val gDocReader: GDocReader,
    private val gDocHelper: GDocHelper,
    private val siteOptions: SiteOptions,
    private val indexReader: IndexReader,
    private val entityFactory: EntityFactory,
    gDocReaderOptions: GDocReaderOptions,
): CliktCommand(name = "preload", help = "Download from GDocs") {
    companion object {
        private val TAG = PreloadCommand::class.java.simpleName
    }

    @Suppress("unused")
    private val _gDocReaderOptions by gDocReaderOptions

    override fun run() {
        gDocReader.init()
        timing.get("Total").time {
            // Fetch the index.
            val site = Site(entityFactory.index(siteOptions.indexGdocId))
            gDocHelper.preload(site.index)
            indexReader.readIndex(site)
            // Fetch content for article and blog entries to cache them.
            site.articleEntries.forEach {
                logger.d(TAG, "Cache article entity: ${it.entity.fileId}")
                gDocHelper.preload(it.entity)
                it.entity.getContent()
                assert(it.entity.isAvailable)
            }
            site.blogEntries.forEach {
                logger.d(TAG, "Cache blog entity: ${it.entity.fileId}")
                gDocHelper.preload(it.entity)
                it.entity.getContent()
                assert(it.entity.isAvailable)
            }
        }
        timing.printToLog()
    }
}
