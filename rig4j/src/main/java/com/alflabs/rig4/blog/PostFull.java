package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public class PostFull implements Comparable<PostFull> {
    public final PostTree.FileItem mFileItem;
    public final SourceContent mContent;
    public final String mCategory;
    public final String mKey;
    public final LocalDate mDate;
    public final String mTitle;
    private PostFull mPrevFull;
    private PostFull mNextFull;

    public PostFull(
            @NonNull BlogPage parent,
            @NonNull String category,
            @NonNull String key,
            @NonNull LocalDate date,
            @NonNull String title,
            @NonNull SourceContent content) {
        mCategory = category;
        mKey = key;
        mDate = date;
        mTitle = title;
        mFileItem = new PostTree.FileItem(parent.getFileItem().getDir(), mKey + PostTree.HTML);
        mContent = content;
    }

    @Null
    public PostFull getPrevFull() {
        return mPrevFull;
    }

    @Null
    public PostFull getNextFull() {
        return mNextFull;
    }

    public void setPrevFull(PostFull prevFull) {
        mPrevFull = prevFull;
    }

    public void setNextFull(PostFull nextFull) {
        mNextFull = nextFull;
    }

    @Override
    public int compareTo(PostFull other) {
        return mKey.compareTo(other.mKey);
    }

    public File prepareHtmlDestFile(Blog blog, BlogGenerator.Generator generator) throws IOException {
        File destFile = new File(generator.getDestDir(), mFileItem.getLeafFile());
        generator.getFileOps().createParentDirs(destFile);

        mContent.setTransformer(generator.getLazyHtmlTransformer(destFile, "postFull:"));
        blog.getBlogHeader().setTransformer(generator.getLazyHtmlTransformer(destFile, "postFullHeader:"));
        return destFile;
    }
}
