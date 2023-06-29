package com.alflabs.rig4k.common

import com.alflabs.utils.FakeFileOps
import com.alflabs.utils.ILogger
import com.alflabs.utils.MockClock
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

class HashStoreTest {
    @get:Rule var mockitoRule: MockitoRule = MockitoJUnit.rule()

    private val logger : ILogger = mock()
    private val options : BlobStoreOptions = mock()
    private lateinit var store: HashStore

    @Before
    fun setUp() {
        val timing = Timing(MockClock(), logger)
        val fileOps = FakeFileOps()
        val blobStore = BlobStore(options, fileOps, timing, logger)
        store = HashStore(logger, blobStore)
    }

    @Test
    fun testString() {
        Truth.assertThat(store.getString("key")).isNull()
        store.putString("key", "content")
        Truth.assertThat(store.getString("key")).isEqualTo("content")
    }
}
