package com.alflabs.rig4k.dl

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DlCommand @Inject constructor(
    private val gDocReader: GDocReader
): CliktCommand(name = "dl", help = "Download from GDocs") {
    private val gDocReaderOptions by GDocReaderOptions()


    override fun run() {
        gDocReader.init(gDocReaderOptions)
        println("@@ Rig4k-DL run")
    }

}
