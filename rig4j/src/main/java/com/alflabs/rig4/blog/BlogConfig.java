package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.flags.Flags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A "blog config" represents a full independent blog site with its own configuration
 * variables: categories accept/reject, banner name, and whether to generate single vs mixed
 * pages.
 * <p/>
 * Various blogs can be generated from the same config and they all use the same
 * configuration variables.
 * <p/>
 * The configuration variable have defaults in Rig4j, which are then overridden by the
 * values in the rig42rc file, which are then overridden by the Izu tags in the parsed source
 * files.
 */
public class BlogConfig {
    private String mMixedCat;
    private final Map<String, CatFilter> mFilters = new HashMap<>();

    public BlogConfig(@NonNull Flags flags) {
        mMixedCat = flags.getString(BlogFlags.BLOG_MIXED_CAT);
        for (String flag : BlogFlags.FILTER_FLAGS) {
            mFilters.put(flag, new CatFilter(flags.getString(flag)));
        }
    }

    @SuppressWarnings("UnnecessaryLabelOnContinueStatement")
    public void updateFrom(@NonNull List<String> tags) {
        String izuMixedCatTag = IzuTags.PREFIX + BlogFlags.BLOG_MIXED_CAT + IzuTags.PARAM_SEP;

        nextTag: for (String tag : tags) {
            if (tag.startsWith(izuMixedCatTag)) {
                String value = tag.substring(izuMixedCatTag.length()).trim();
                if (!value.isEmpty()) {
                    mMixedCat = value;
                }
                continue nextTag;
            }

            for (String flag : BlogFlags.FILTER_FLAGS) {
                String izuTag = IzuTags.PREFIX + flag + IzuTags.PARAM_SEP;

                if (tag.startsWith(izuTag)) {
                    String value = tag.substring(izuTag.length()).trim();
                    if (!value.isEmpty()) {
                        mFilters.put(flag, new CatFilter(value));
                    }
                    continue nextTag;
                }
            }
        }
    }

    public CatFilter getCatAcceptFilter() {
        return mFilters.get(BlogFlags.BLOG_ACCEPT_CAT);
    }

    public CatFilter getCatRejectFilter() {
        return mFilters.get(BlogFlags.BLOG_REJECT_CAT);
    }

    public CatFilter getCatBannerFilter() {
        return mFilters.get(BlogFlags.BLOG_BANNER_EXCLUDE);
    }

    public CatFilter getGenSingleFilter() {
        return mFilters.get(BlogFlags.BLOG_GEN_SINGLE);
    }

    public CatFilter getGenMixedFilter() {
        return mFilters.get(BlogFlags.BLOG_GEN_MIXED);
    }

    public String getMixedCat() {
        return mMixedCat;
    }
}
