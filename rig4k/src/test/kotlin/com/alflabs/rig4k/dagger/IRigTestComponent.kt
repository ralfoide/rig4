package com.alflabs.rig4k.dagger

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    FakeClockModule::class,
    FakeFileOpsModule::class,
    LoggerModule::class,
    JsonFactoryModule::class])
interface IRigTestComponent : IRigComponent {

//    fun inject(test: ScriptTest2kBase)

    @Component.Factory
    interface Factory {
        fun createComponent(): IRigTestComponent
    }
}
