package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** A blog in the PostTree. */
public class Blog {
    private final static String TAG = Blog.class.getSimpleName();
    private final String mCategory;
    private final String mTitle;
    private final SourceContent mBlogHeader;
    /**
     * mBlogIndex is page in mBlogPages.
     */
    private final BlogPage mBlogIndex;
    private final List<BlogPage> mBlogPages = new ArrayList<>();

    public Blog(@NonNull String category, @Null String title, @Null SourceContent blogHeader) {
        mCategory = category;
        mTitle = title == null ? "" : title;
        mBlogHeader = blogHeader != null ? blogHeader : new SourceContent();
        mBlogIndex = new BlogPage(this, new File(PostTree.BLOG_ROOT, category));
        mBlogPages.add(mBlogIndex);
    }

    @NonNull
    public String getCategory() {
        return mCategory;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    @NonNull
    public BlogPage getBlogIndex() {
        return mBlogIndex;
    }

    @NonNull
    public List<BlogPage> getBlogPages() {
        return mBlogPages;
    }

    @NonNull
    public SourceContent getBlogHeader() {
        return mBlogHeader;
    }

    public void generate(@NonNull BlogGenerator.Generator generator) throws Exception {

        AtomWriter atomWriter = new AtomWriter();
        atomWriter.write(this, generator, new PostTree.FileItem(mBlogIndex.getFileItem().getDir(), PostTree.ATOM_XML));

        PostFull nextFull = null;
        for (int i = mBlogPages.size() - 1; i >= 0; i--) {
            nextFull = mBlogPages.get(i).computeNextFullPage(nextFull);
        }

        PostFull prevFull = null;
        for (int i = 0; i < mBlogPages.size(); i++) {
            prevFull = mBlogPages.get(i).computePrevFullPage(prevFull);
            mBlogPages.get(i).generate(i, mBlogPages, generator);
        }
    }
}
