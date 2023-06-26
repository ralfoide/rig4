package com.alflabs.rig4k.main

import com.alflabs.rig4k.common.SomethingCommon
import com.alflabs.rig4k.dl.DlEntryPoint
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

class Rig4kCommand : CliktCommand() {
    private var dlEntryPoint: DlEntryPoint
    private val verbose by option(help = "Verbose mode").flag()
    private val configPath by option("-c", "--config", help = "Path to configuration file")
        .file(mustExist = true, mustBeReadable = true)
        .required()

    init {
        println("@@ MAIN init EntryPoint")
        dlEntryPoint = DlEntryPoint()
        subcommands(dlEntryPoint)
    }

    override fun run() {
        println("@@ MAIN RUN do Something")
        SomethingCommon().doSomething()
        dlEntryPoint.doSomething()
    }
}
