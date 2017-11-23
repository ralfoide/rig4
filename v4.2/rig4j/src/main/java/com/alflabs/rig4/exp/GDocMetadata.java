package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GDocMetadata {
    @NonNull
    public static GDocMetadata create(@NonNull String title,@NonNull String contentHash) {
        return new AutoValue_GDocMetadata(title, contentHash);
    }

    @NonNull
    public abstract String getTitle();

    @NonNull
    public abstract String getContentHash();
}
