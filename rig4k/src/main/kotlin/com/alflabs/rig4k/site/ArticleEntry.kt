package com.alflabs.rig4k.site

import com.alflabs.rig4k.dl.ArticleEntity

data class ArticleEntry(
    /** The entity source of the article.  */
    val entity: ArticleEntity,

    /**
     * The destination file path, with optional _forward_ sub-directories (".." is not allowed).
     * All paths are denoted using an OS-agnostic forward-slash.
     */
    val destName: String,
)
