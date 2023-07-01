package com.alflabs.rig4k.site

data class ArticleEntry(
    /** The gdoc id for the source of the article.  */
    val fileId: String,

    /**
     * The destination file path, with optional _forward_ sub-directories (".." is not allowed).
     * All paths are denoted using an OS-agnostic forward-slash.
     */
    val destName: String,
)
