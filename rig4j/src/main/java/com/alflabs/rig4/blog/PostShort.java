package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;

import java.io.File;
import java.time.LocalDate;

public class PostShort implements Comparable<PostShort> {
    public final SourceContent mContent;
    public final PostFull mPostFull;
    public final String mCategory;
    public final String mKey;
    public final LocalDate mDate;
    public final String mTitle;
    public final boolean mReadMoreLink;

    public PostShort(
            @NonNull String category,
            @NonNull String key,
            @NonNull LocalDate date,
            @NonNull String title,
            @NonNull SourceContent content,
            @NonNull PostFull postFull,
            boolean readMoreLink) {
        mCategory = category;
        mKey = key;
        mDate = date;
        mTitle = title;
        mContent = content;
        mPostFull = postFull;
        mReadMoreLink = readMoreLink;
    }

    @Override
    public int compareTo(PostShort other) {
        return mKey.compareTo(other.mKey);
    }

    public void prepareContent(
            BlogGenerator.Generator generator,
            @NonNull File destFile) {
        this.mContent.setTransformer(generator.getLazyHtmlTransformer(destFile, "postShort:" + mKey + ":"));
    }
}
