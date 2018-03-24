package com.alflabs.rig4.struct;

import com.alflabs.rig4.gdoc.GDocMetadata;

public class GDocEntity {
    private final GDocMetadata mMetadata;
    private final boolean mUpdateToDate;
    private final byte[] mContent;

    public GDocEntity(GDocMetadata metadata, boolean updateToDate, byte[] content) {
        mMetadata = metadata;
        mUpdateToDate = updateToDate;
        mContent = content;
    }

    public GDocMetadata getMetadata() {
        return mMetadata;
    }

    public boolean isUpdateToDate() {
        return mUpdateToDate;
    }

    public byte[] getContent() {
        return mContent;
    }
}
