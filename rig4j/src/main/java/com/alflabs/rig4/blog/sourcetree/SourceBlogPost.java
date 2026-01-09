package com.alflabs.rig4.blog.sourcetree;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.blog.BlogSourceParser;
import com.alflabs.rig4.blog.IzuTags;
import org.apache.commons.codec.digest.DigestUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SourceBlogPost implements Comparable<SourceBlogPost> {
    private final String mCategory;
    private final LocalDate mDate;
    private final String mTitle;
    private final SourceContent mShortContent;
    private final SourceContent mFullContent;
    private final String mKey;

    public SourceBlogPost(
            @NonNull String category,
            @NonNull LocalDate date,
            @NonNull String title,
            @Null SourceContent shortContent,
            @NonNull SourceContent fullContent) {
        mCategory = category;
        mDate = date;
        mTitle = title;
        mShortContent = shortContent;
        mFullContent = fullContent;

        String key = mDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                + "_"
                + title.trim().toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9_-]", "_")
                .replaceAll("_+", "_");

        // If the title is longer than the max length, truncate it and append the first few
        // hex characters of the title hash. This ensures we have a reasonable length for the
        // generated post filenames. SHA-1 is enough as collisions are unlikely and would only
        // become relevant if the titles had the same 40 first characters (which is considered
        // to be unlikely by itself).
        final int maxKeyLen = 48;
        final int maxShaLen = 8;
        if (key.length() > maxKeyLen) {
            String shaHex = DigestUtils.sha1Hex(key);
            key = key.substring(0, maxKeyLen - 1 - maxShaLen) + "_" + shaHex.substring(0, maxShaLen);
        }

        mKey = key;
    }

    public String getCategory() {
        return mCategory;
    }

    @NonNull
    public String getKey() {
        return mKey;
    }

    @NonNull
    public LocalDate getDate() {
        return mDate;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    @Null
    public SourceContent getShortContent() {
        return mShortContent;
    }

    @NonNull
    public SourceContent getFullContent() {
        return mFullContent;
    }

    @NonNull
    public static SourceBlogPost from(@NonNull String mainBlogCategory,
                                      @NonNull BlogSourceParser.ParsedSection section)
            throws BlogSourceParser.ParseException {

        String postCategory = IzuTags.getTagValue(IzuTags.IZU_CATEGORY, section.getIzuTags());
        if (postCategory.isEmpty() && IzuTags.hasPrefixTag(IzuTags.IZU_CATEGORY, section.getIzuTags())) {
            throw new BlogSourceParser.ParseException(
                    String.format("Invalid tag '%s' in section [%s:%s]",
                            IzuTags.IZU_CATEGORY,
                            section.getDate(),
                            section.getTextTitle()));
        }

        return new SourceBlogPost(
                postCategory.isEmpty() ? mainBlogCategory : postCategory,
                section.getDate(),
                section.getTextTitle(),
                SourceContent.from(section.getIntermediaryShort()),
                SourceContent.from(section.getIntermediaryFull()));
    }

    @Override
    public int compareTo(SourceBlogPost other) {
        return this.mKey.compareTo(other.mKey);
    }
}
