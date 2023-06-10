package com.alflabs.rig4.gdoc;

import com.alflabs.annotations.NonNull;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
public abstract class GDocMetadata {
    @NonNull
    public static GDocMetadata create(
            @NonNull String title,
            @NonNull String contentHash,
            @NonNull Map<String, String> exportLinks) {
        return new AutoValue_GDocMetadata(title, contentHash, exportLinks);
    }

    @NonNull
    public abstract String getTitle();

    @NonNull
    public abstract String getContentHash();

    @NonNull
    public abstract Map<String, String> getExportLinks();
}
