package com.alflabs.rig4k.dl

data class GDocMetadata(
    val title: String,
    val contentHash: String,
    val exportLinks: Map<String, String>)
