package com.alflabs.rig4.exp;

import com.alflabs.annotations.ExportedJson;
import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@ExportedJson
public class GDocMetadata {
    @JsonProperty("title")
    private final String mTitle;

    @JsonProperty("hash")
    private final String mContentHash;

    @JsonIgnore
    private byte[] mContent;

    @JsonCreator
    public GDocMetadata(
            @JsonProperty("title") @NonNull String title,
            @JsonProperty("hash") @NonNull String contentHash) {
        mTitle = title;
        mContentHash = contentHash;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    @NonNull
    public String getContentHash() {
        return mContentHash;
    }

    @Null
    public byte[] getContent() {
        return mContent;
    }

    public void setContent(@Null byte[] content) {
        mContent = content;
    }
}
