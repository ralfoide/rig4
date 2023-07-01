package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.Timing
import com.alflabs.rig4k.site.Site
import com.alflabs.rig4k.site.SiteOptions
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreloadCommand @Inject constructor(
    private val timing: Timing,
    private val gDocReader: GDocReader,
    private val gDocHelper: GDocHelper,
    private val siteOptions: SiteOptions,
    private val indexReader: IndexReader,
    gDocReaderOptions: GDocReaderOptions,
): CliktCommand(name = "preload", help = "Download from GDocs") {
    @Suppress("unused")
    private val _gDocReaderOptions by gDocReaderOptions

    override fun run() {
        gDocReader.init()
        println("@@ Rig4k-DL run")
        timing.get("Total").time {
            // Always fetch the index
            val site = Site(IndexEntity(siteOptions.indexGdocId))
            gDocHelper.preload(site.index)
            indexReader.readIndex(site)

//            // Only fetch other documents if they are not up-to-date.
//            index.articleEntries.forEach { entry ->
//                val entity = gDocHelper.getGDocAsync(entry.fileId, "text/html")
//                entity?.let {
//                }
//            }

            println("@@ read site: $site")
        }
        timing.printToLog()
    }

}
