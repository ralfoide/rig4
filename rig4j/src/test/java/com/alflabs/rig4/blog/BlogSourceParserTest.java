package com.alflabs.rig4.blog;

import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.exp.HtmlTransformer;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.StringLogger;
import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

public class BlogSourceParserTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private Timing mTiming;
    @Mock private Timing.TimeAccumulator mTimeAccumulator;
    @Mock private FileOps mFileOps;
    @Mock private HashStore mHashStore;

    private final StringLogger mLogger = new StringLogger();

    private HtmlTransformer mHtmlTransformer;
    private BlogSourceParser mBlogSourceParser;

    @Before
    public void setUp() throws Exception {
        when(mTiming.get("HtmlTransformer")).thenReturn(mTimeAccumulator);
        mHtmlTransformer = new HtmlTransformer(new Flags(mFileOps, mLogger), mTiming, mHashStore);
        mBlogSourceParser = new BlogSourceParser(mHtmlTransformer);
    }

    @Test
    public void testSourceParser() throws Exception {
        String source = ""
                + "[izu:blog] [izu:cat:testing]\n"
                + "Header content\n"
                + "[izu:header:end]\n"
                + "[s:1901-01-01] Title 1\n"
                + "Post 1 Content\n"
                + "[izu:break]\n"
                + "Post 1 long version\n"
                + "[s:1901-01-02:Title 2] Whatever\n"
                + "Post 2 Content\n"
                + "Post 2 is short\n"
                + "[izu:blog:end]\n"
                + "[s:1901-01-03:Title 3] Work in progress\n"
                + "This post is not ready\n";

        source =
            "<html><body>" +
                Arrays
                .stream(source.split("\n"))
                .map(s -> "<span>" + s + "</span>")
                .collect(Collectors.joining("\n"))
            + "</body></html>";

        BlogSourceParser.ParsedResult result =
                mBlogSourceParser.parse(source.getBytes(Charsets.UTF_8));

        assertThat(result).isNotNull();
        assertThat(result.getTags()).containsAllOf(
                IzuTags.IZU_BLOG,
                IzuTags.IZU_CATEGORY + "testing");
        assertThat(result.getBlogCategory()).isEqualTo("testing");

        assertThat(result.getIntermediaryHeader()).isNotNull();
        assertThat(result.getIntermediaryHeader().html()).isEqualTo(
                "<span>[izu:blog] [izu:cat:testing]</span> \n" +
                "<span>Header content</span> \n" +
                "<span>[izu:header:end]</span>");
        assertThat(result.getParsedSections()).hasSize(2);

        BlogSourceParser.ParsedSection section1 = result.getParsedSections().get(0);
        BlogSourceParser.ParsedSection section2 = result.getParsedSections().get(1);

        assertThat(section1.getDate()).isEqualTo(LocalDate.of(1901, 1, 1));
        assertThat(section1.getTextTitle()).isEqualTo("Title 1");
        assertThat(section1.getIntermediaryShort().html()).isEqualTo(
                "<span>Post 1 Content</span> \n" +
                "<span>[izu:break]</span>");
        assertThat(section1.getIntermediaryfull().html()).isEqualTo(
                "<span>Post 1 Content</span> \n" +
                "<span>[izu:break]</span> \n" +
                "<span>Post 1 long version</span>");

        assertThat(section2.getDate()).isEqualTo(LocalDate.of(1901, 1, 2));
        assertThat(section2.getTextTitle()).isEqualTo("Title 2");
        assertThat(section2.getIntermediaryShort()).isNull();
        assertThat(section2.getIntermediaryfull().html()).isEqualTo(
                "<span>Post 2 Content</span> \n" +
                "<span>Post 2 is short</span> \n" +
                "<span>[izu:blog:end]</span>");
    }
}
