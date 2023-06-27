package com.alflabs.rig4k.main

import com.alflabs.rig4k.dagger.DaggerIRigComponent

class MainEntryPoint {
    companion object {
        @JvmStatic
        fun main(args: Array<out String>) {
            val component = DaggerIRigComponent.factory().createComponent()
            component.rig4kCommand.main(args)
        }
    }
}
