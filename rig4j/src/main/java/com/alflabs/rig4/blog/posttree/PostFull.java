package com.alflabs.rig4.blog.posttree;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.blog.BlogGenerator;
import com.alflabs.rig4.blog.sourcetree.SourceContent;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PostFull implements Comparable<PostFull> {
    public final PostTree.FileItem mFileItem;
    public final List<PostTree.FileItem> mFileRedirects = new ArrayList<>();
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
            @NonNull List<String> redirects,
            @NonNull SourceContent content) {
        File dir = parent.getFileItem().getDir();

        mCategory = category;
        mKey = key;
        mDate = date;
        mTitle = title;
        mFileItem = new PostTree.FileItem(dir, mKey + PostTree.HTML);
        mContent = content;

        for (String redirectKey : redirects) {
            mFileRedirects.add(new PostTree.FileItem(dir, redirectKey + PostTree.HTML));
        }
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
        File destFile = getHtmlDestFile(mFileItem, generator);
        mContent.setTransformer(generator.getLazyHtmlTransformer(destFile, "postFull:"));
        blog.getBlogHeader().setTransformer(generator.getLazyHtmlTransformer(destFile, "postFullHeader:"));
        return destFile;
    }

    public File getHtmlDestFile(PostTree.FileItem fileItem, BlogGenerator.Generator generator) throws IOException {
        File destFile = new File(generator.getDestDir(), fileItem.getLeafFile());
        generator.getFileOps().createParentDirs(destFile);
        return destFile;
    }
}
