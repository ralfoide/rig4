package com.alflabs.rig4k.site

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteOptions @Inject constructor(): OptionGroup("Site Options") {
    val indexGdocId by option(help = "Index GDoc ID.")
        .required()

    val destDir by option(help = "Destination directory.")
        .file(canBeDir = true, mustBeWritable = true)
        .required()

    val ga4Id by option(help = "GA4 ID")

    val siteTitle by option(help = "Web site title")
        .default("Site Title")

    val siteBanner by option(help = "Web site banner filename")
        .default("header.jpg")

    val siteBaseUrl by option(help = "Web site base URL")
        .required()

    val rewrittenUrl by option(help = "Root URL rewritten to site URL for staging")
}
