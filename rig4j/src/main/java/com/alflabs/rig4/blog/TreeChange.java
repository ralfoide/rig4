package com.alflabs.rig4.blog;

public abstract class TreeChange {
    private boolean mChanged;

    public boolean isChanged() {
        return mChanged;
    }

    public abstract boolean isTreeChanged();

    public void setChanged(boolean changed) {
        mChanged = changed;
    }
}
