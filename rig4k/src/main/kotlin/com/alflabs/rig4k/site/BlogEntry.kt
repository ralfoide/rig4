package com.alflabs.rig4k.site

import com.alflabs.rig4k.dl.BlogEntity

/**
 * A blog entry in the main index file: "blog config-number gdoc-file-id".
 *
 * This value structure indicates which gdoc file id to read and
 * to which [BlogConfig] it corresponds.
 */
data class BlogEntry(
    val entity: BlogEntity,
    val configNumber: Int,
)
