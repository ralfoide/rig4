package com.alflabs.rig4k.common

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlobStoreOptions @Inject constructor(): OptionGroup("BlobStore Options") {
    val blobStoreDir by option(
        help = "Directory where Rig4k caches local data.")
        .default("~/.rig42k/blob_store")
}
