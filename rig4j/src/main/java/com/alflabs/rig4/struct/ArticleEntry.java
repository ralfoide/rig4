package com.alflabs.rig4.struct;

import com.alflabs.annotations.NonNull;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ArticleEntry {
    public static ArticleEntry create(@NonNull String fileId, @NonNull String destName) {
        return new AutoValue_ArticleEntry(fileId, destName);
    }

    /** The gdoc id for the source of the article. */
    public abstract String getFileId();
    /**
     * The destination file path, with optional _forward_ sub-directories (".." is not allowed).
     * All paths are denoted using an OS-agnostic forward-slash.
     */
    public abstract String getDestName();
}
