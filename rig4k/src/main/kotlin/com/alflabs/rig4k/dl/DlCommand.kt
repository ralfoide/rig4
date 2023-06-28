package com.alflabs.rig4k.dl

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DlCommand @Inject constructor(
    private val gDocReader: GDocReader,
    gDocReaderOptions: GDocReaderOptions,
): CliktCommand(name = "dl", help = "Download from GDocs") {
    @Suppress("unused")
    private val _gDocReaderOptions by gDocReaderOptions

    override fun run() {
        gDocReader.init()
        println("@@ Rig4k-DL run")
    }

}
