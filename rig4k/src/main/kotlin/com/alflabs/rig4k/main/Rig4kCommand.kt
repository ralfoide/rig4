package com.alflabs.rig4k.main

import com.alflabs.rig4k.common.BlobStoreOptions
import com.alflabs.rig4k.dl.PreloadCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.sources.PropertiesValueSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Rig4kCommand @Inject constructor(
    dlCommand: PreloadCommand,
    mainOptions: MainOptions,
    blobStoreOptions: BlobStoreOptions,
) : NoOpCliktCommand() {
    @Suppress("unused")
    private val _mainOptions by mainOptions
    private val _blobStoreOptions by blobStoreOptions

    init {
        println("@@ MAIN init EntryPoint")
        context { valueSource = PropertiesValueSource.from("rig42k.rc") }
        subcommands(dlCommand)
    }
}
