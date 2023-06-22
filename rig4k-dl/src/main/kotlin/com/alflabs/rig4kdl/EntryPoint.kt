package com.alflabs.rig4kdl

class EntryPoint {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello World ${args.contentToString()}")
            val ep = EntryPoint()
            ep.doSomething()
        }
    }

    init {
        println("@@ init EntryPoint")
    }

    fun doSomething() {
        println("@@ do Something")
    }

}
