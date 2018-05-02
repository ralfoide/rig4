package com.alflabs.rig4;


import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.FakeFileOps;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.MockClock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.truth.Truth.assertThat;

public class HashStoreTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private Flags mFlags;
    @Mock private ILogger mLogger;

    private HashStore mStore;

    @Before
    public void setUp() throws Exception {
        Timing timing = new Timing(new MockClock(), mLogger);
        FakeFileOps fileOps = new FakeFileOps();
        BlobStore blobStore = new BlobStore(mFlags, fileOps, timing, mLogger);

        mStore = new HashStore(blobStore);
    }

    @Test
    public void testString() throws Exception {
        assertThat(mStore.getString("key")).isNull();

        mStore.putString("key", "content");
        assertThat(mStore.getString("key")).isEqualTo("content");
    }
}
