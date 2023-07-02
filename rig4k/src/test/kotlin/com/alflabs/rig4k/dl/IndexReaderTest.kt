package com.alflabs.rig4k.dl

import com.alflabs.rig4k.site.ArticleEntry
import com.alflabs.rig4k.site.BlogEntry
import com.alflabs.rig4k.site.Site
import com.alflabs.utils.ILogger
import com.google.common.base.Charsets
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

class IndexReaderTest {
    @get:Rule var mockitoRule: MockitoRule = MockitoJUnit.rule()

    private val logger : ILogger = Mockito.mock()
    private lateinit var reader : IndexReader

    @Before
    fun setUp() {
        reader = IndexReader(logger)
    }

    @Test
    fun testReadIndex_empty() {
        val site = Site(IndexEntity("indexId"))
        site.index.preloadForTesting(
            GDocMetadata("index", "index metadata hash", emptyMap()),
            "".toByteArray(Charsets.UTF_8)
        )

        reader.readIndex(site)
        assertThat(site.articleEntries).isEmpty()
        assertThat(site.blogEntries).isEmpty()
    }

    @Test
    fun testReadIndex() {
        val content = """
            file1.html   01234567_file1
            file2.html   23456789_file2
            subdir/file3.html   34567890_file3
            blog         id_cat_1
            blog         id_cat_2
            blog.html    45678901_file4
            
            """.trimIndent()
        val site = Site(IndexEntity("indexId"))
        site.index.preloadForTesting(
            GDocMetadata("index", "index metadata hash", emptyMap()),
            content.toByteArray(Charsets.UTF_8)
        )

        reader.readIndex(site)
        assertThat(site.articleEntries).containsExactly(
            ArticleEntry("01234567_file1", "file1.html"),
            ArticleEntry("23456789_file2", "file2.html"),
            ArticleEntry("34567890_file3", "subdir/file3.html"),
            ArticleEntry("45678901_file4", "blog.html")
        )
        assertThat(site.blogEntries).containsExactly(
            BlogEntry("id_cat_1", 0),
            BlogEntry("id_cat_2", 0)
        )
    }

    @Test
    fun testReadBlogEntries() {
        val content = """
            blog           12345
            Blog           23456
            blog (desc)    34567
            blog 1         45678
            Blog 234       56789
            blog 56 (desc) 67890
            
            """.trimIndent()
        val site = Site(IndexEntity("indexId"))
        site.index.preloadForTesting(
            GDocMetadata("index", "index metadata hash", emptyMap()),
            content.toByteArray(Charsets.UTF_8)
        )

        reader.readIndex(site)
        assertThat(site.blogEntries).containsExactly(
            BlogEntry("12345", 0),
            BlogEntry("23456", 0),
            BlogEntry("34567", 0),
            BlogEntry("45678", 1),
            BlogEntry("56789", 234),
            BlogEntry("67890", 56)
        )
    }
}
