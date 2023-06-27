package com.alflabs.rig4k.dagger

import com.alflabs.rig4k.main.Rig4kCommand
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ClockModule::class,
    FileOpsModule::class,
    LoggerModule::class,
    JsonFactoryModule::class])
interface IRigComponent {
    val rig4kCommand: Rig4kCommand

    @Component.Factory
    interface Factory {
        /*@BindsInstance rig4kCommand: Rig4kCommand*/
        fun createComponent(): IRigComponent
    }
}
