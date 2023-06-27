package com.alflabs.rig4k.dagger

import com.alflabs.rig4k.main.Rig4kCommand
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [RigModule::class])
interface IRigComponent {
    val rig4kCommand: Rig4kCommand

    @Component.Factory
    interface Factory {
        fun createComponent(/*@BindsInstance rig4kCommand: Rig4kCommand*/): IRigComponent
    }
}
