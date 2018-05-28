package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.rig4.struct.BlogEntry;
import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * A map of config-number to {@link BlogConfig}.
 * <p/>
 * Each config is a set of differently generated blogs with their own configuration variables.
 * This does not indicate which sources files to read.
 */
public class BlogConfigs {
    private final Map<Integer, BlogConfig> mBlogConfigs = new TreeMap<>();

    public void add(@NonNull Flags flags, int section) {
        mBlogConfigs.computeIfAbsent(section, (i) -> new BlogConfig(flags));
    }

    @NonNull
    public BlogConfig get(@NonNull BlogEntry entry) {
        return Preconditions.checkNotNull(mBlogConfigs.get(entry.getConfigNumber()));
    }

    public Collection<BlogConfig> iter() {
        return mBlogConfigs.values();
    }
}
