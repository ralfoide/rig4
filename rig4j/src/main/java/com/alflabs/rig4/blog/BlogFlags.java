package com.alflabs.rig4.blog;

import com.alflabs.rig4.flags.Flags;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BlogFlags {
    public static final String BLOG_ACCEPT_CAT = "blog-accept-cat";
    public static final String BLOG_EXCLUDE_CAT = "blog-exclude-cat";
    public static final String BLOG_GEN_SINGLE = "blog-gen-single";
    public static final String BLOG_BANNER_EXCLUDE = "blog-banner-exclude";
    public static final String BLOG_GEN_MIXED = "blog-gen-mixed";

    private final Flags mFlags;

    @Inject
    public BlogFlags(Flags flags) {
        mFlags = flags;
    }

    public void declareFlags() {
        mFlags.addString(BLOG_ACCEPT_CAT,       ".*",   "A regexp list of categories to accept from sources.");
        mFlags.addString(BLOG_EXCLUDE_CAT,      "",     "A regexp list of categories to exclude from sources.");
        mFlags.addString(BLOG_GEN_SINGLE,       ".*",   "A regexp list of categories for which to generate single category pages.");
        mFlags.addString(BLOG_BANNER_EXCLUDE,   "",     "A regexp list of categories for which to generate banner links.");
        mFlags.addString(BLOG_GEN_MIXED,        ".*",   "A regexp list of categories to include in the mixed-categories pages.");
    }
}
