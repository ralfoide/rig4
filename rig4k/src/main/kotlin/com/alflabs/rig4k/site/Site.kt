package com.alflabs.rig4k.site

import com.alflabs.rig4k.dl.IndexEntity

class Site(
    val index: IndexEntity
) {
    val articleEntries = mutableListOf<ArticleEntry>()
    val blogEntries = mutableListOf<BlogEntry>()
}
