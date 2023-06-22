package com.alflabs.rig4k.main

import com.alflabs.rig4k.common.SomethingCommon
import com.alflabs.rig4k.dl.DlEntryPoint

class MainEntryPoint {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello World ${args.contentToString()}")
            val ep = MainEntryPoint()
            ep.doSomething()
        }
    }

    init {
        println("@@ MAIN init EntryPoint")
    }

    fun doSomething() {
        println("@@ MAIN do Something")
        SomethingCommon().doSomething()
        DlEntryPoint().doSomething()
    }

}
