package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.exp.Templater;
import com.alflabs.utils.FileOps;
import com.google.common.base.Charsets;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BlogPage {
    private final static String TAG = BlogPage.class.getSimpleName();
    private final Blog mBlog;
    private final PostTree.FileItem mFileItem;
    private final List<PostShort> mPostShorts = new ArrayList<>();
    private final List<PostFull> mPostFulls = new ArrayList<>();

    /**
     * Create a top-level index page.
     */
    public BlogPage(@NonNull Blog blog, @NonNull File dir) {
        mBlog = blog;
        mFileItem = new PostTree.FileItem(dir, PostTree.INDEX_HTML);
    }

    /**
     * Create a numbered index page.
     */
    public BlogPage(@NonNull Blog blog, @NonNull BlogPage parent, int index) {
        mBlog = blog;
        mFileItem = new PostTree.FileItem(parent.getFileItem().getDir(), String.format("%04x", index) + PostTree.HTML);
    }

    @NonNull
    public PostTree.FileItem getFileItem() {
        return mFileItem;
    }

    @NonNull
    public List<PostFull> getPostFulls() {
        return mPostFulls;
    }

    @NonNull
    public List<PostShort> getPostShorts() {
        return mPostShorts;
    }

    /**
     * Fill the page with the given posts.
     * The input collection should already be ordered as it should be presented on the page.
     */
    public void fillFrom(@NonNull Collection<SourceTree.BlogPost> sourcePosts) {
        for (SourceTree.BlogPost sourcePost : sourcePosts) {
            SourceTree.Content fullContent = sourcePost.getFullContent();
            SourceTree.Content shortContent = sourcePost.getShortContent();
            boolean readMoreLink = true;
            if (shortContent == null) {
                shortContent = fullContent;
                readMoreLink = false;
            }

            PostFull postFull = new PostFull(
                    this,
                    sourcePost.getCategory(),
                    sourcePost.getKey(),
                    sourcePost.getDate(),
                    sourcePost.getTitle(),
                    fullContent);

            PostShort postShort = new PostShort(
                    sourcePost.getCategory(),
                    sourcePost.getKey(),
                    sourcePost.getDate(),
                    sourcePost.getTitle(),
                    shortContent,
                    postFull,
                    readMoreLink);

            mPostFulls.add(postFull);
            mPostShorts.add(postShort);
        }

        // The posts comparator compares using ascending keys. We want to descending order.
        mPostFulls.sort(Collections.reverseOrder());
        mPostShorts.sort(Collections.reverseOrder());
    }

    public void generate(int index,
                         @NonNull List<BlogPage> blogPages,
                         @NonNull BlogGenerator.Generator generator) throws Exception {
        // Write one file with all the short entries.
        File mainFile = generateMainPage(index, blogPages, generator);

        // Write one file per full entry.
        for (PostFull postFull : mPostFulls) {
            generateFullPage(generator, postFull, mainFile);
        }
    }

    public PostFull computePrevFullPage(PostFull lastFull) {
        for (PostFull postFull : mPostFulls) {
            postFull.setPrevFull(lastFull);
            lastFull = postFull;
        }
        return lastFull;
    }

    public PostFull computeNextFullPage(PostFull lastFull) {
        for (int i = mPostFulls.size() - 1; i >= 0; i--) {
            PostFull postFull = mPostFulls.get(i);
            postFull.setNextFull(lastFull);
            lastFull = postFull;
        }
        return lastFull;
    }

    private File generateMainPage(int index,
                                  @NonNull List<BlogPage> blogPages,
                                  @NonNull BlogGenerator.Generator generator) throws Exception {
        File destFile = new File(generator.getDestDir(), mFileItem.getLeafFile());

        generator.getFileOps().createParentDirs(destFile);
        generator.getLogger().d(TAG, "--- Generate  Page: " + generator.categoryToHtml(mBlog.getCategory())
                + ", file: " + destFile);

        SourceTree.Content blogHeader = mBlog.getBlogHeader();
        blogHeader.setTransformer(generator.getLazyHtmlTransformer(destFile, "pageHeader:" + index + ":"));
        // This is an index page so we get the description & images from the blog header if any
        // and as a fall-back get them from the posts content.
        String formattedHeader = blogHeader.getFormatted();
        String relImageLink = blogHeader.getFormattedFirstImageSrc();
        // TODO for now there isn't a good way to generate *good* descriptions or get them from a blog-level izu tag.
        // (The way I write the blog, the first extracted paragraph isn't a good match).
        String headDescription = null;

        StringBuilder allPostsContent = new StringBuilder();
        for (PostShort postShort : mPostShorts) {
            allPostsContent.append(generateShort(generator, destFile, postShort));

            if (relImageLink == null) {
                relImageLink = postShort.mContent.getFormattedFirstImageSrc();
            }
            if (headDescription == null) {
                headDescription = postShort.mContent.getFormattedDescription();
            }
        }

        String prevPageLink = null;
        String nextPageLink = null;
        if (index > 0) {
            prevPageLink = blogPages.get(index - 1).mFileItem.getName();
        }
        if (index < blogPages.size() - 1) {
            nextPageLink = blogPages.get(index + 1).mFileItem.getName();
        }

        Templater.BlogPageData templateData = new Templater.BlogPageData(
                /* isIndex= */ true,
                generator.getSiteTitle(),
                generator.getAbsSiteLink(),
                generator.getRevSiteLink(),
                mFileItem.getLeafDirWeb(),
                generator.getRelBannerLink(),
                generator.getSiteCss(),
                generator.getGAUid(),
                mBlog.getTitle(),
                destFile.getName(), // page filename (for base-url/page-filename.html)
                prevPageLink,
                nextPageLink,
                formattedHeader,
                "",                 // no post title for an index
                "",                 // no post date  for an index
                "",                 // no post category for an index
                "",                 // no post cat link for an index
                destFile.getName(),
                allPostsContent.toString(),
                generator.getGenInfo(),
                relImageLink,
                headDescription);

        String generated = generator.getTemplater().generate(templateData);
        String genHash = generator.getTemplater().hashContent(generated);

        FileOps fileOps = generator.getFileOps();
        HashStore hashStore = generator.getHashStore();

        // We need to write the file if it's missing or if it has changed.
        boolean shouldWrite = !fileOps.isFile(destFile);
        if (!shouldWrite) {
            shouldWrite = !genHash.equals(hashStore.getString(destFile.getPath()));
        }

        if (shouldWrite) {
            hashStore.putString(destFile.getPath(), genHash);
            fileOps.writeBytes(generated.getBytes(Charsets.UTF_8), destFile);
            generator.getLogger().d(TAG, "        Write Page: file " + destFile);
        }

        return destFile;
    }

    private String generateShort(
            @NonNull BlogGenerator.Generator generator,
            @NonNull File destFile,
            @NonNull PostShort postData)
            throws Exception {
        generator.getLogger().d(TAG, "    Generate Short: " + postData.mKey);

        postData.mContent.setTransformer(generator.getLazyHtmlTransformer(destFile, "postShort:" + postData.mKey + ":"));

        String fullLink = postData.mPostFull.mFileItem.getName();

        Templater.BlogPostData templateData = new Templater.BlogPostData(
                generator.getAbsSiteLink(),
                generator.getRevSiteLink(),
                mFileItem.getLeafDirWeb(),
                postData.mTitle,
                postData.mDate.toString(),
                generator.categoryToHtml(postData.mCategory),
                generator.linkForCategory(postData.mCategory),
                fullLink,
                postData.mReadMoreLink ? fullLink : null,
                postData.mContent.getFormatted()
        );

        return generator.getTemplater().generate(templateData);
    }

    private void generateFullPage(
            @NonNull BlogGenerator.Generator generator,
            @NonNull PostFull postData,
            @NonNull File mainFile)
            throws Exception {
        File destFile = postData.prepareHtmlDestFile(mBlog, generator);

        generator.getLogger().d(TAG, "    Generate  Full: " + postData.mKey);

        String prevPageLink = postData.getPrevFull() == null
                ? null
                : postData.getPrevFull().mFileItem.getName();
        String nextPageLink = postData.getNextFull() == null
                ? null
                : postData.getNextFull().mFileItem.getName();

        String content = postData.mContent.getFormatted();
        String relImageLink = postData.mContent.getFormattedFirstImageSrc();
        String headDescription = postData.mContent.getFormattedDescription();

        Templater.BlogPageData templateData = new Templater.BlogPageData(
                /* isIndex= */ false,
                generator.getSiteTitle(),
                generator.getAbsSiteLink(),
                generator.getRevSiteLink(),
                mFileItem.getLeafDirWeb(),
                generator.getRelBannerLink(),
                generator.getSiteCss(),
                generator.getGAUid(),
                mBlog.getTitle(),
                mainFile.getName(),  // main page links to the index containing this full page
                prevPageLink,
                nextPageLink,
                mBlog.getBlogHeader().getFormatted(),
                postData.mTitle,
                postData.mDate.toString(),
                generator.categoryToHtml(postData.mCategory),
                generator.linkForCategory(postData.mCategory),
                destFile.getName(),  // post full page should link to its own url
                content,
                generator.getGenInfo(),
                relImageLink,
                headDescription
        );

        String generated = generator.getTemplater().generate(templateData);
        String genHash = generator.getTemplater().hashContent(generated);

        FileOps fileOps = generator.getFileOps();
        HashStore hashStore = generator.getHashStore();

        // We need to write the file if it's missing or if it has changed.
        boolean shouldWrite = !fileOps.isFile(destFile);
        if (!shouldWrite) {
            shouldWrite = !genHash.equals(hashStore.getString(destFile.getPath()));
        }

        if (shouldWrite) {
            hashStore.putString(destFile.getPath(), genHash);
            fileOps.writeBytes(generated.getBytes(Charsets.UTF_8), destFile);
            generator.getLogger().d(TAG, "       Write  Full: " + destFile);
        }
    }
}
