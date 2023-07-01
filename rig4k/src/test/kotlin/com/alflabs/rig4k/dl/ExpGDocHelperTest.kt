package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.BlobStore
import com.alflabs.rig4k.common.HashStore
import com.alflabs.rig4k.common.Timing
import com.alflabs.utils.FakeFileOps
import com.alflabs.utils.ILogger
import com.alflabs.utils.MockClock
import com.google.common.base.Charsets
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.FileNotFoundException

class ExpGDocHelperTest {
    @get:Rule var mockitoRule: MockitoRule = MockitoJUnit.rule()

    private val logger : ILogger = mock()
    private val gDocReader : GDocReader = mock()
    private val blobStore : BlobStore = mock()
    private val hashStore : HashStore = mock()
    private val timing = Timing(MockClock(), logger)
    private val fileOps = FakeFileOps()
    private lateinit var helper: ExpGDocHelper

    @Before
    fun setUp() {
        helper = ExpGDocHelper(logger, fileOps, timing, gDocReader, blobStore, hashStore)
    }

    // --- GetGDocSync
    @Test
    fun testGetGDocSync_invalidId() {
        whenever(blobStore.getBytes(any()))
            .thenThrow(FileNotFoundException())
        whenever(hashStore.getString(any()))
            .thenThrow(FileNotFoundException())
        whenever(gDocReader.getMetadataById(any()))
            .thenThrow(FileNotFoundException())
        whenever(
            gDocReader.readFileById(
                any(),
                any()
            )
        ).thenThrow(FileNotFoundException())
        val entity = helper.getGDocSync("gdoc id", "text/html")
        assertThat(entity).isNull()
    }

    @Test
    fun testGetGDocSync_notCached() {
        whenever(blobStore.getBytes(any()))
            .thenThrow(FileNotFoundException())
        whenever(hashStore.getString(any()))
            .thenThrow(FileNotFoundException())
        val gDocMetadata = GDocMetadata("gdoc title", "gdoc content hash", emptyMap())
        val contentBytes = "GDoc File Content".toByteArray(Charsets.UTF_8)
        whenever(gDocReader.getMetadataById("gdoc id"))
            .thenReturn(gDocMetadata)
        whenever(gDocReader.readFileById("gdoc id", "text/html"))
            .thenReturn(contentBytes)
        val entity = helper.getGDocSync("gdoc id", "text/html")
        assertThat(entity).isNotNull()
        assertThat(entity!!.metadata).isSameInstanceAs(gDocMetadata)
        assertThat(entity.getContent()).isEqualTo(contentBytes)

        // Check the store for content. It's missing so the hash is never read from the store.
        verify(blobStore).getBytes("gdoc-content-gdoc id-text/html")
        verify(hashStore, never()).getString("gdoc-hash-gdoc id")

        // GDoc is fetched for both metadata and content
        verify(gDocReader).getMetadataById("gdoc id")
        verify(gDocReader).readFileById("gdoc id", "text/html")

        // Update the store for both metadata and content
        verify(hashStore).putString("gdoc-hash-gdoc id", "gdoc content hash")
        verify(blobStore).putBytes("gdoc-content-gdoc id-text/html", contentBytes)
    }

    @Test
    fun testGetGDocSync_cachedAndSame() {
        val contentBytes = "GDoc File Content".toByteArray(Charsets.UTF_8)
        whenever(blobStore.getBytes("gdoc-content-gdoc id-text/html"))
            .thenReturn(contentBytes)
        whenever(hashStore.getString("gdoc-hash-gdoc id")).thenReturn("gdoc content hash")
        val gDocMetadata = GDocMetadata("gdoc title", "gdoc content hash", emptyMap())
        whenever(gDocReader.getMetadataById("gdoc id"))
            .thenReturn(gDocMetadata)
        whenever(gDocReader.readFileById("gdoc id", "text/html"))
            .thenReturn(contentBytes)
        val entity = helper.getGDocSync("gdoc id", "text/html")
        assertThat(entity).isNotNull()
        assertThat(entity!!.metadata).isSameInstanceAs(gDocMetadata)
        assertThat(entity.getContent()).isEqualTo(contentBytes)

        // Check the store for content and metadata
        verify(blobStore).getBytes("gdoc-content-gdoc id-text/html")
        verify(hashStore).getString("gdoc-hash-gdoc id")

        // GDoc is fetched for both metadata but not content (since the content hash matches)
        verify(gDocReader).getMetadataById("gdoc id")
        verify(gDocReader, never()).readFileById("gdoc id", "text/html")

        // The stores are not updated since the data has not changed
        verify(hashStore, never())
            .putString("gdoc-hash-gdoc id", "gdoc content hash")
        verify(blobStore, never())
            .putBytes("gdoc-content-gdoc id-text/html", contentBytes)
    }

    @Test
    fun testGetGDocSync_cachedAndDifferent() {
        val oldContentBytes = "OLD GDoc File Content".toByteArray(Charsets.UTF_8)
        val newContentBytes = "NEW GDoc File Content".toByteArray(Charsets.UTF_8)
        val oldContentHash = "OLD content hash"
        val newContentHash = "NEW content hash"
        whenever(blobStore.getBytes("gdoc-content-gdoc id-text/html"))
            .thenReturn(oldContentBytes)
        whenever(hashStore.getString("gdoc-hash-gdoc id")).thenReturn(oldContentHash)
        val newMetadata = GDocMetadata("gdoc title", newContentHash, emptyMap())
        whenever(gDocReader.getMetadataById("gdoc id"))
            .thenReturn(newMetadata)
        whenever(gDocReader.readFileById("gdoc id", "text/html"))
            .thenReturn(newContentBytes)
        val entity = helper.getGDocSync("gdoc id", "text/html")
        assertThat(entity).isNotNull()
        assertThat(entity!!.metadata).isSameInstanceAs(newMetadata)
        assertThat(entity.getContent()).isEqualTo(newContentBytes)

        // Check the store for content and metadata
        verify(blobStore).getBytes("gdoc-content-gdoc id-text/html")
        verify(hashStore).getString("gdoc-hash-gdoc id")

        // GDoc is fetched for both metadata and content (since the content hash differs)
        verify(gDocReader).getMetadataById("gdoc id")
        verify(gDocReader).readFileById("gdoc id", "text/html")

        // Update the store for both metadata and content
        verify(hashStore).putString("gdoc-hash-gdoc id", newContentHash)
        verify(blobStore).putBytes("gdoc-content-gdoc id-text/html", newContentBytes)
    }

    // --- GetGDocAsync
    @Test
    fun testGetGDocAsync_invalidId() {
        whenever(blobStore.getBytes(any()))
            .thenThrow(FileNotFoundException())
        whenever(hashStore.getString(any()))
            .thenThrow(FileNotFoundException())
        whenever(gDocReader.getMetadataById(any()))
            .thenThrow(FileNotFoundException())
        whenever(
            gDocReader.readFileById(
                any(),
                any()
            )
        ).thenThrow(FileNotFoundException())
        val entity = helper.getGDocAsync("gdoc id", "text/html")
        assertThat(entity).isNull()
    }

    @Test
    fun testGetGDocAsync_notCached() {
        whenever(blobStore.getBytes(any()))
            .thenThrow(FileNotFoundException())
        whenever(hashStore.getString(any()))
            .thenThrow(FileNotFoundException())
        val gDocMetadata = GDocMetadata("gdoc title", "gdoc content hash", emptyMap())
        val contentBytes = "GDoc File Content".toByteArray(Charsets.UTF_8)
        whenever(gDocReader.getMetadataById("gdoc id"))
            .thenReturn(gDocMetadata)
        whenever(gDocReader.readFileById("gdoc id", "text/html"))
            .thenReturn(contentBytes)
        val entity = helper.getGDocAsync("gdoc id", "text/html")
        assertThat(entity).isNotNull()
        assertThat(entity!!.metadata).isSameInstanceAs(gDocMetadata)

        // Content is not retrieved yet
        verify(gDocReader).getMetadataById("gdoc id")
        verify(gDocReader, never()).readFileById(any(), any())

        // Store has not been updated yet
        verify(hashStore, never()).putString(any(), any())
        verify(blobStore, never()).putBytes(any(), any())

        // Retrieve the content now, which triggers a fetch from gdoc
        assertThat(entity.getContent()).isEqualTo(contentBytes)
        verify(gDocReader).readFileById("gdoc id", "text/html")

        // Store still has not been updated yet
        verify(hashStore, never()).putString(any(), any())
        verify(blobStore, never()).putBytes(any(), any())

        // Sync to the store now.
        entity.syncToStore()

        // Update the store for both metadata and content
        verify(hashStore).putString("gdoc-hash-gdoc id", "gdoc content hash")
        verify(blobStore).putBytes("gdoc-content-gdoc id-text/html", contentBytes)
    }
}
