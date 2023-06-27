package com.alflabs.rig4k.main

import com.alflabs.rig4k.dagger.DaggerIRigTestComponent
import com.alflabs.rig4k.dagger.IRigTestComponent
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class EntryPointTest {

    private lateinit var component: IRigTestComponent

    @Before
    fun setUp() {
        component = DaggerIRigTestComponent.factory().createComponent()
    }

    @Test
    fun test1() {
        component.rig4kCommand.main(emptyList())
        assertThat(component.rig4kCommand.verbose).isFalse()
    }
}
