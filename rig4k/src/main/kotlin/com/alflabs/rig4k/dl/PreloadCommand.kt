package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.Timing
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreloadCommand @Inject constructor(
    private val timing: Timing,
    private val gDocReader: GDocReader,
    private val siteOptions: SiteOptions,
    gDocReaderOptions: GDocReaderOptions,
): CliktCommand(name = "preload", help = "Download from GDocs") {
    @Suppress("unused")
    private val _gDocReaderOptions by gDocReaderOptions

    override fun run() {
        gDocReader.init()
        println("@@ Rig4k-DL run")
        timing.get("Total").time {
            println("@@ read index: ${siteOptions.indexGdocId}")
        }
        timing.printToLog()
    }

}
