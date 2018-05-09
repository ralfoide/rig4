package com.alflabs.rig4.exp;


import com.alflabs.rig4.gdoc.GDocHelper;
import com.alflabs.rig4.gdoc.GDocMetadata;
import com.alflabs.rig4.struct.ArticleEntry;
import com.alflabs.rig4.struct.BlogEntry;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.rig4.struct.Index;
import com.alflabs.utils.ILogger;
import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

public class IndexReaderTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private ILogger mLogger;
    @Mock private GDocHelper mGDocHelper;
    private IndexReader mReader;

    @Before
    public void setUp() throws Exception {
        mReader = new IndexReader(mLogger, mGDocHelper);
    }

    @Test
    public void testReadIndex_empty() throws Exception {
        GDocMetadata gDocMetadata = GDocMetadata.create("index", "index metadata hash");
        GDocEntity entity = new GDocEntity(gDocMetadata, false /* updateToDate */,
                "".getBytes(Charsets.UTF_8));
        when(mGDocHelper.getGDocSync("indexId", "text/plain")).thenReturn(entity);

        Index index = mReader.readIndex("indexId");
        assertThat(index).isNotNull();
        assertThat(index.getArticleEntries()).isEmpty();
        assertThat(index.getBlogEntries()).isEmpty();
    }

    @Test
    public void testReadIndex() throws Exception {
        String content =
                "file1.html   01234567_file1\n" +
                "file2.html   23456789_file2\n" +
                "blog         id_cat_1\n" +
                "blog         id_cat_2\n" +
                "blog.html    34567890_file3\n";
        GDocMetadata gDocMetadata = GDocMetadata.create("index", "index metadata hash");
        GDocEntity entity = new GDocEntity(gDocMetadata, false /* updateToDate */,
                content.getBytes(Charsets.UTF_8));
        when(mGDocHelper.getGDocSync("indexId", "text/plain")).thenReturn(entity);

        Index index = mReader.readIndex("indexId");
        assertThat(index).isNotNull();
        assertThat(index.getArticleEntries()).containsAllOf(
                ArticleEntry.create("01234567_file1", "file1.html"),
                ArticleEntry.create("23456789_file2", "file2.html"),
                ArticleEntry.create("34567890_file3", "blog.html")
        );
        assertThat(index.getBlogEntries()).containsAllOf(
                BlogEntry.create("id_cat_1", 0),
                BlogEntry.create("id_cat_2", 0)
        );
    }

    @Test
    public void testRedBlogEntries() throws Exception {
        String content =
                "blog           12345\n" +
                "Blog           23456\n" +
                "blog (desc)    34567\n" +
                "blog 1         45678\n" +
                "Blog 234       56789\n" +
                "blog 56 (desc) 67890\n";
        GDocMetadata gDocMetadata = GDocMetadata.create("index", "index metadata hash");
        GDocEntity entity = new GDocEntity(gDocMetadata, false /* updateToDate */,
                content.getBytes(Charsets.UTF_8));
        when(mGDocHelper.getGDocSync("indexId", "text/plain")).thenReturn(entity);

        Index index = mReader.readIndex("indexId");
        assertThat(index).isNotNull();
        assertThat(index.getBlogEntries()).containsAllOf(
                BlogEntry.create("12345",   0),
                BlogEntry.create("23456",   0),
                BlogEntry.create("34567",   0),
                BlogEntry.create("45678",   1),
                BlogEntry.create("56789", 234),
                BlogEntry.create("67890",  56)
        );
    }
}
