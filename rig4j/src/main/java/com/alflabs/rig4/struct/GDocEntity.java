package com.alflabs.rig4.struct;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.gdoc.GDocMetadata;

public class GDocEntity {
    private final GDocMetadata mMetadata;
    private final boolean mUpdateToDate;
    private final ContentFetcher mFetcher;
    private final Syncer mSyncToStore;
    private byte[] mContent;
    private boolean mContentFetched;

    public GDocEntity(GDocMetadata metadata, boolean updateToDate, byte[] content) {
        mMetadata = metadata;
        mUpdateToDate = updateToDate;
        mSyncToStore = null;
        mContent = content;
        mFetcher = null;
    }

    public GDocEntity(GDocMetadata metadata, boolean updateToDate,
                      ContentFetcher contentFetcher,
                      Syncer syncToStore) {
        mMetadata = metadata;
        mUpdateToDate = updateToDate;
        mSyncToStore = syncToStore;
        mContent = null;
        mFetcher = contentFetcher;
    }

    public GDocMetadata getMetadata() {
        return mMetadata;
    }

    public boolean isUpdateToDate() {
        return mUpdateToDate;
    }

    public boolean isContentFetched() {
        return mContentFetched;
    }

    public byte[] getContent() {
        if (mContent == null && mFetcher != null && !mContentFetched) {
            mContent = mFetcher.fetchContent(this);
            mContentFetched = true;
        }
        return mContent;
    }

    public void syncToStore() {
        if (mSyncToStore != null) {
            mSyncToStore.sync(this);
        }
    }

    public interface Syncer {
        void sync(GDocEntity entity);
    }

    public interface ContentFetcher {
        @NonNull
        byte[] fetchContent(GDocEntity entity);
    }
}
