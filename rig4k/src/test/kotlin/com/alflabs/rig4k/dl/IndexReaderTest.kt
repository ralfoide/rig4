package com.alflabs.rig4k.dl

import com.alflabs.rig4k.dagger.DaggerIRigTestComponent
import com.alflabs.rig4k.dagger.IRigTestComponent
import com.alflabs.rig4k.site.ArticleEntry
import com.alflabs.rig4k.site.BlogEntry
import com.alflabs.rig4k.site.Site
import com.google.common.base.Charsets
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import javax.inject.Inject

class IndexReaderTest {
    @get:Rule var mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var component: IRigTestComponent
    @Inject lateinit var entityFactory: EntityFactory
    @Inject lateinit var reader : IndexReader

    @Before
    fun setUp() {
        component = DaggerIRigTestComponent.factory().createComponent()
        component.inject(this)
    }

    @Test
    fun testReadIndex_empty() {
        val site = Site(entityFactory.index("indexId"))
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
        val site = Site(entityFactory.index("indexId"))
        site.index.preloadForTesting(
            GDocMetadata("index", "index metadata hash", emptyMap()),
            content.toByteArray(Charsets.UTF_8)
        )

        reader.readIndex(site)
        assertThat(site.articleEntries).containsExactly(
            ArticleEntry(entityFactory.article("01234567_file1"), "file1.html"),
            ArticleEntry(entityFactory.article("23456789_file2"), "file2.html"),
            ArticleEntry(entityFactory.article("34567890_file3"), "subdir/file3.html"),
            ArticleEntry(entityFactory.article("45678901_file4"), "blog.html")
        )
        assertThat(site.blogEntries).containsExactly(
            BlogEntry(entityFactory.blog("id_cat_1"), 0),
            BlogEntry(entityFactory.blog("id_cat_2"), 0)
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
        val site = Site(entityFactory.index("indexId"))
        site.index.preloadForTesting(
            GDocMetadata("index", "index metadata hash", emptyMap()),
            content.toByteArray(Charsets.UTF_8)
        )

        reader.readIndex(site)
        assertThat(site.blogEntries).containsExactly(
            BlogEntry(entityFactory.blog("12345"), 0),
            BlogEntry(entityFactory.blog("23456"), 0),
            BlogEntry(entityFactory.blog("34567"), 0),
            BlogEntry(entityFactory.blog("45678"), 1),
            BlogEntry(entityFactory.blog("56789"), 234),
            BlogEntry(entityFactory.blog("67890"), 56)
        )
    }
}
