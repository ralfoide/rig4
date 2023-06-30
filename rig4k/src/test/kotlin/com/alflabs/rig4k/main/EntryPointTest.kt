package com.alflabs.rig4k.main

import com.alflabs.rig4k.dagger.DaggerIRigTestComponent
import com.alflabs.rig4k.dagger.IRigTestComponent
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.inject.Inject

class EntryPointTest {
    @get:Rule var tempFolder = TemporaryFolder()

    private lateinit var component: IRigTestComponent
    @Inject lateinit var mainOptions: MainOptions

    @Before
    fun setUp() {
        component = DaggerIRigTestComponent.factory().createComponent()
        component.inject(this)
    }

    @Test
    fun testVerbose() {
        assertThrows(PrintHelpMessage::class.java) {
            // parse() does not call exit(), unlike main().
            component.rig4kCommand.parse(listOf(
                "--verbose",
                "--index-gdoc-id", "index_gdoc_id",
                "--dest-dir", tempFolder.root.path,
                "--site-base-url", "http://example.com/",
            ))
            fail("parse did not throw PrintHelpMessage exception as expected")
        }
        assertThat(mainOptions.verbose).isTrue()
    }
}
