package com.alflabs.rig4k.dagger

import com.alflabs.rig4k.main.EntryPointTest
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    FakeClockModule::class,
    FakeFileOpsModule::class,
    LoggerModule::class,
    JsonFactoryModule::class])
interface IRigTestComponent : IRigComponent {
    fun inject(entryPointTest: EntryPointTest)

    @Component.Factory
    interface Factory {
        fun createComponent(): IRigTestComponent
    }
}
