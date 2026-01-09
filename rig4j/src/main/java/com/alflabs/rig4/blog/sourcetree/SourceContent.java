package com.alflabs.rig4.blog.sourcetree;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.exp.HtmlTransformer;
import com.google.common.base.Preconditions;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Intermediary HTML content that has been partially provided by the HtmlTransformer yet
 * is not fully formatted, as the formatting depends on the context where the content
 * is going to be used (e.g. to generate image assets in the right directories).
 */
public class SourceContent {
    private final Element mIntermediary;
    private Element mFormatted;
    private String mTransformerKey;
    private HtmlTransformer.LazyTransformer mTransformer;

    /**
     * Creates an empty content.
     * <p/>
     * Use {@link #from(Element)} to create content from an existing {@link Element} node.
     */
    public SourceContent() {
        mFormatted = null;
        mIntermediary = null;
    }

    /**
     * Creates content with either fully formatted content or intermediary content yet to be formatted.
     * One or the other should be provided.
     */
    private SourceContent(/*@Null String formatted,*/ @Null Element intermediary) {
        mFormatted = null;
        mIntermediary = intermediary;
    }

    /**
     * Transformer needed to transform the intermediary content into formatted content.
     */
    public void setTransformer(@NonNull HtmlTransformer.LazyTransformer transformer) {
        mTransformer = transformer;
        String newKey = transformer.getTransformKey();
        if (mFormatted != null && (mTransformerKey == null || !mTransformerKey.equals(newKey))) {
            mFormatted = null;
        }
        mTransformerKey = newKey;
    }

    /**
     * Returns the formatted content.
     * If a transformer is available, intermediary content is transformed first.
     * Each transformer has a key; when a new transformer is set, the content is regenerated
     * if the key does not match.
     */
    @Null
    public String getFormatted() throws IOException, URISyntaxException {
        if (mFormatted == null && mIntermediary != null) {
            Preconditions.checkNotNull(mTransformer);
            mFormatted = mTransformer.lazyTransform(mIntermediary);
        }
        return mFormatted == null ? null : mFormatted.html();
    }

    /**
     * Extracts the first "img src" attribute found in the formatted content.
     * {@link #getFormatted()} must have been called first to generate the formatted content.
     */
    @Null
    public String getFormattedFirstImageSrc() {
        if (mFormatted != null) {
            Preconditions.checkNotNull(mTransformer);
            return mTransformer.getFormattedFirstImageSrc(mFormatted);
        }
        return null;
    }

    /**
     * Extracts the first paragraph description found in the formatted content.
     * {@link #getFormatted()} must have been called first to generate the formatted content.
     */
    @Null
    public String getFormattedDescription() {
        if (mFormatted != null) {
            Preconditions.checkNotNull(mTransformer);
            return mTransformer.getFormattedDescription(mFormatted);
        }
        return null;
    }

    @Null
    public Element getIntermediary() {
        return mIntermediary;
    }

    @Null
    public static SourceContent from(@Null Element intermediary) {
        return intermediary == null ? null : new SourceContent(intermediary);
    }
}
