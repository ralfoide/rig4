package com.alflabs.rig4k.main

import com.alflabs.rig4k.common.BlobStoreOptions
import com.alflabs.rig4k.dl.PreloadCommand
import com.alflabs.rig4k.dl.TransformCommand
import com.alflabs.rig4k.site.SiteOptions
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.sources.PropertiesValueSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Rig4kCommand @Inject constructor(
    preloadCommand: PreloadCommand,
    transformCommand: TransformCommand,
    mainOptions: MainOptions,
    siteOptions: SiteOptions,
    blobStoreOptions: BlobStoreOptions,
) : NoOpCliktCommand() {
    @Suppress("unused")
    private val _mainOptions by mainOptions
    private val _blobStoreOptions by blobStoreOptions
    private val _siteOptions by siteOptions

    init {
        subcommands(preloadCommand, transformCommand)
        context { valueSource = PropertiesValueSource.from(".rig42krc") }
    }
}
