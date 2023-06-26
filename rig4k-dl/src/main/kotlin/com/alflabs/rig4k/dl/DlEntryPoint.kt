package com.alflabs.rig4k.dl

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class DlEntryPoint : CliktCommand(name = "dl", help = "Download from GDocs") {
    val someDlParam by option(help="Help for dl param").required()

    init {
        println("@@ DL init EntryPoint")
    }

    override fun run() {
        println("@@ Rig4k-DL run")
    }

    fun doSomething() {
        println("@@ Rig4k-DL do Something")
    }

}
