package com.alflabs.rig4.struct;

import com.alflabs.annotations.NonNull;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class BlogEntry {
    public static BlogEntry create(@NonNull String fileId, int siteNumber) {
        return new AutoValue_BlogEntry(fileId, siteNumber);
    }

    public abstract String getFileId();
    public abstract int getSiteNumber();
}
