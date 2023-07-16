package com.alflabs.rig4.exp;

import com.alflabs.rig4.BlobStore;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.FakeFileOps;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.MockClock;
import com.alflabs.utils.StringLogger;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;


public class HtmlTransformerTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    private ILogger mLogger = new StringLogger();
    private FileOps mFileOps = new FakeFileOps();
    private Flags mFlags = new Flags(mFileOps, mLogger);
    private Timing mTiming = new Timing(new MockClock(), mLogger);
    private BlobStore mBlobStore = new BlobStore(mFlags, mFileOps, mTiming, mLogger);
    private HashStore mHashStore = new HashStore(mLogger, mBlobStore);

    private HtmlTransformer mTransformer;

    @Before
    public void setUp() throws Exception {
        mBlobStore.declareFlags();
        new ExpFlags(mFlags).declareFlags();
        mFlags.parseCommandLine(new String[] {
                "--blob-store-dir=/tmp/blog-store-dir",
                "--exp-rewritten-url=http://example.com/"
        });
        mTransformer = new HtmlTransformer(mFlags, mTiming, mHashStore);
    }

    private Element transform(Element intermediary) throws IOException, URISyntaxException {
        HtmlTransformer.LazyTransformer processor = mTransformer.createLazyTransformer("dest-key", new HtmlTransformer.Callback() {
            @Override
            public String processDrawing(String id, int width, int height, boolean useCache) throws IOException {
                return "[drawing for " + id + "]";
            }

            @Override
            public String processImage(URI uri, int width, int height, boolean useCache) throws IOException {
                return "[image for " + uri.toString() + "]";
            }
        });
        return processor.lazyTransform(intermediary);
    }

    @Test
    public void testRemoveStyles() throws IOException, URISyntaxException {
        String source = "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<body>" +
                "<div class=\"container center-horiz\">\n" +
                "\n" +
                "<p style=\" line-height: 1; orphans: 2; padding-bottom: 0pt; widows: 2\">" +
                "<span style=\"font-size: 11pt;color: rgb(0, 0, 0)\"> </span></p>\n" +
                "</div></body></html";

        Element intermediary = mTransformer.simplifyForProcessing(source.getBytes(StandardCharsets.UTF_8));
        Element transformed = transform(intermediary);
        String content = transformed.html();

        assertThat(content).isEqualTo(
                "<div>\n" +
                " <p><span> </span></p>\n" +
                "</div>");
    }
}
