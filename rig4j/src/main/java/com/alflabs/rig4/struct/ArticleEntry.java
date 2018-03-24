package com.alflabs.rig4.struct;

import com.alflabs.annotations.NonNull;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ArticleEntry {
    public static ArticleEntry create(@NonNull String fileId, @NonNull String destName) {
        return new AutoValue_ArticleEntry(fileId, destName);
    }

    public abstract String getFileId();
    public abstract String getDestName();
}
