package com.alflabs.rig4.struct;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.blog.BlogConfig;
import com.google.auto.value.AutoValue;

/**
 * A blog entry in the main index file: "blog config-number gdoc-file-id".
 * <p/>
 * This value structure indicates which gdoc file id to read and
 * to which {@link BlogConfig} it corresponds.
 */
@AutoValue
public abstract class BlogEntry {
    public static BlogEntry create(@NonNull String fileId, int configNumber) {
        return new AutoValue_BlogEntry(fileId, configNumber);
    }

    public abstract String getFileId();
    public abstract int getConfigNumber();
}
