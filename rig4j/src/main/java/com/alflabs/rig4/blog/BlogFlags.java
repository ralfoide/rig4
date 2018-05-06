package com.alflabs.rig4.blog;

import com.alflabs.rig4.flags.Flags;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BlogFlags {
    public static final String BLOG_ACCEPT_CAT = "blog-accept-cat";
    public static final String BLOG_REJECT_CAT = "blog-reject-cat";
    public static final String BLOG_GEN_SINGLE = "blog-gen-single";
    public static final String BLOG_BANNER_EXCLUDE = "blog-banner-exclude";
    public static final String BLOG_GEN_MIXED = "blog-gen-mixed";
    public static final String BLOG_MIXED_CAT = "blog-mixed-cat";

    private final Flags mFlags;

    @Inject
    public BlogFlags(Flags flags) {
        mFlags = flags;
    }

    public void declareFlags() {
        mFlags.addString(BLOG_ACCEPT_CAT,       ".*",   "A regexp list of categories to accept from sources.");
        mFlags.addString(BLOG_REJECT_CAT,       "",     "A regexp list of categories to reject from sources.");
        mFlags.addString(BLOG_GEN_SINGLE,       ".*",   "A regexp list of categories for which to generate single category pages.");
        mFlags.addString(BLOG_BANNER_EXCLUDE,   "",     "A regexp list of categories for which to generate banner links.");
        mFlags.addString(BLOG_GEN_MIXED,        ".*",   "A regexp list of categories to include in the mixed-categories pages.");
        mFlags.addString(BLOG_MIXED_CAT,        "all",  "Synthetic category name used for the mixed-categories pages.");
    }
}
