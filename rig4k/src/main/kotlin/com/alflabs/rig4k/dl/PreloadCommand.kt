package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.Timing
import com.alflabs.rig4k.site.SiteOptions
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreloadCommand @Inject constructor(
    private val timing: Timing,
    private val gDocHelper: ExpGDocHelper,
    private val gDocReader: GDocReader,
    private val siteOptions: SiteOptions,
    private val indexReader: ExpIndexReader,
    gDocReaderOptions: GDocReaderOptions,
): CliktCommand(name = "preload", help = "Download from GDocs") {
    @Suppress("unused")
    private val _gDocReaderOptions by gDocReaderOptions

    override fun run() {
        gDocReader.init()
        println("@@ Rig4k-DL run")
        timing.get("Total").time {
            // Always fetch the index
            val index = indexReader.readIndex(siteOptions.indexGdocId)

            // Only fetch other documents if they are not up-to-date.
            index.articleEntries.forEach { entry ->
                val entity = gDocHelper.getGDocAsync(entry.fileId, "text/html")
                entity?.let {
                }
            }

            println("@@ read index: $index")
        }
        timing.printToLog()
    }

}
