package com.alflabs.rig4k.main

import com.alflabs.rig4k.dl.DlCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Rig4kCommand @Inject constructor(
    private val dlCommand: DlCommand,
) : NoOpCliktCommand() {
    val verbose by option(help = "Verbose mode").flag()
    val configPath by option("-c", "--config", help = "Path to configuration file")

    init {
        println("@@ MAIN init EntryPoint")
        subcommands(dlCommand)
    }
}
