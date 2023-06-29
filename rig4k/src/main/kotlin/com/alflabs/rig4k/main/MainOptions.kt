package com.alflabs.rig4k.main

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainOptions @Inject constructor(): OptionGroup("Main Options") {
    val verbose by option(help = "Verbose mode").flag()
}
