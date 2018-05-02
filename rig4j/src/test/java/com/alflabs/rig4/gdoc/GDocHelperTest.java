package com.alflabs.rig4.gdoc;

import com.alflabs.rig4.BlobStore;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GDocHelperTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private ILogger mLogger;
    @Mock private FileOps mFileOps;
    @Mock private Timing mTiming;
    @Mock private GDocReader mGDocReader;
    @Mock private BlobStore mBlobStore;
    @Mock private HashStore mHashStore;

    private GDocHelper mHelper;

    @Before
    public void setUp() throws Exception {
        mHelper = new GDocHelper(mLogger, mFileOps, mTiming, mGDocReader, mBlobStore, mHashStore);
    }


    // --- GetGDocSync

    @Test
    public void testGetGDocSync_invalidId() throws Exception {
        when(mBlobStore.getBytes(anyString())).thenThrow(new FileNotFoundException());
        when(mHashStore.getString(anyString())).thenThrow(new FileNotFoundException());
        when(mGDocReader.getMetadataById(anyString())).thenThrow(new FileNotFoundException());
        when(mGDocReader.readFileById(anyString(), anyString())).thenThrow(new FileNotFoundException());

        GDocEntity entity = mHelper.getGDocSync("gdoc id", "text/html");
        assertThat(entity).isNull();
    }

    @Test
    public void testGetGDocSync_notCached() throws Exception {
        when(mBlobStore.getBytes(anyString())).thenThrow(new FileNotFoundException());
        when(mHashStore.getString(anyString())).thenThrow(new FileNotFoundException());

        GDocMetadata gDocMetadata = GDocMetadata.create("gdoc title", "gdoc content hash");
        byte[] contentBytes = "GDoc File Content".getBytes(Charsets.UTF_8);

        when(mGDocReader.getMetadataById("gdoc id")).thenReturn(gDocMetadata);
        when(mGDocReader.readFileById("gdoc id", "text/html"))
                .thenReturn(contentBytes);

        GDocEntity entity = mHelper.getGDocSync("gdoc id", "text/html");
        assertThat(entity).isNotNull();
        assertThat(entity.getMetadata()).isSameAs(gDocMetadata);
        assertThat(entity.getContent()).isEqualTo(contentBytes);

        // Check the store for content. It's missing so the hash is never read from the store.
        verify(mBlobStore).getBytes("gdoc-content-gdoc id-text/html");
        verify(mHashStore, never()).getString("gdoc-hash-gdoc id");

        // GDoc is fetched for both metadata and content
        verify(mGDocReader).getMetadataById("gdoc id");
        verify(mGDocReader).readFileById("gdoc id", "text/html");

        // Update the store for both metadata and content
        verify(mHashStore).putString("gdoc-hash-gdoc id", "gdoc content hash");
        verify(mBlobStore).putBytes("gdoc-content-gdoc id-text/html", contentBytes);
    }

    @Test
    public void testGetGDocSync_cachedAndSame() throws Exception {
        byte[] contentBytes = "GDoc File Content".getBytes(Charsets.UTF_8);
        when(mBlobStore.getBytes("gdoc-content-gdoc id-text/html")).thenReturn(contentBytes);
        when(mHashStore.getString("gdoc-hash-gdoc id")).thenReturn("gdoc content hash");

        GDocMetadata gDocMetadata = GDocMetadata.create("gdoc title", "gdoc content hash");

        when(mGDocReader.getMetadataById("gdoc id")).thenReturn(gDocMetadata);
        when(mGDocReader.readFileById("gdoc id", "text/html"))
                .thenReturn(contentBytes);

        GDocEntity entity = mHelper.getGDocSync("gdoc id", "text/html");
        assertThat(entity).isNotNull();
        assertThat(entity.getMetadata()).isSameAs(gDocMetadata);
        assertThat(entity.getContent()).isEqualTo(contentBytes);

        // Check the store for content and metadata
        verify(mBlobStore).getBytes("gdoc-content-gdoc id-text/html");
        verify(mHashStore).getString("gdoc-hash-gdoc id");

        // GDoc is fetched for both metadata but not content (since the content hash matches)
        verify(mGDocReader).getMetadataById("gdoc id");
        verify(mGDocReader, never()).readFileById("gdoc id", "text/html");

        // The stores are not updated since the data has not changed
        verify(mHashStore, never()).putString("gdoc-hash-gdoc id", "gdoc content hash");
        verify(mBlobStore, never()).putBytes("gdoc-content-gdoc id-text/html", contentBytes);
    }

    @Test
    public void testGetGDocSync_cachedAndDifferent() throws Exception {
        byte[] oldContentBytes = "OLD GDoc File Content".getBytes(Charsets.UTF_8);
        byte[] newContentBytes = "NEW GDoc File Content".getBytes(Charsets.UTF_8);
        final String oldContentHash = "OLD content hash";
        final String newContentHash = "NEW content hash";
        when(mBlobStore.getBytes("gdoc-content-gdoc id-text/html")).thenReturn(oldContentBytes);
        when(mHashStore.getString("gdoc-hash-gdoc id")).thenReturn(oldContentHash);

        GDocMetadata newMetadata = GDocMetadata.create("gdoc title", newContentHash);
        when(mGDocReader.getMetadataById("gdoc id")).thenReturn(newMetadata);
        when(mGDocReader.readFileById("gdoc id", "text/html")).thenReturn(newContentBytes);

        GDocEntity entity = mHelper.getGDocSync("gdoc id", "text/html");
        assertThat(entity).isNotNull();
        assertThat(entity.getMetadata()).isSameAs(newMetadata);
        assertThat(entity.getContent()).isEqualTo(newContentBytes);

        // Check the store for content and metadata
        verify(mBlobStore).getBytes("gdoc-content-gdoc id-text/html");
        verify(mHashStore).getString("gdoc-hash-gdoc id");

        // GDoc is fetched for both metadata and content (since the content hash differs)
        verify(mGDocReader).getMetadataById("gdoc id");
        verify(mGDocReader).readFileById("gdoc id", "text/html");

        // Update the store for both metadata and content
        verify(mHashStore).putString("gdoc-hash-gdoc id", newContentHash);
        verify(mBlobStore).putBytes("gdoc-content-gdoc id-text/html", newContentBytes);
    }

    // --- GetGDocAsync

    @Test
    public void testGetGDocAsync_invalidId() throws Exception {
        when(mBlobStore.getBytes(anyString())).thenThrow(new FileNotFoundException());
        when(mHashStore.getString(anyString())).thenThrow(new FileNotFoundException());
        when(mGDocReader.getMetadataById(anyString())).thenThrow(new FileNotFoundException());
        when(mGDocReader.readFileById(anyString(), anyString())).thenThrow(new FileNotFoundException());

        GDocEntity entity = mHelper.getGDocAsync("gdoc id", "text/html");
        assertThat(entity).isNull();
    }

    @Test
    public void testGetGDocAsync_notCached() throws Exception {
        when(mBlobStore.getBytes(anyString())).thenThrow(new FileNotFoundException());
        when(mHashStore.getString(anyString())).thenThrow(new FileNotFoundException());

        GDocMetadata gDocMetadata = GDocMetadata.create("gdoc title", "gdoc content hash");
        byte[] contentBytes = "GDoc File Content".getBytes(Charsets.UTF_8);

        when(mGDocReader.getMetadataById("gdoc id")).thenReturn(gDocMetadata);
        when(mGDocReader.readFileById("gdoc id", "text/html"))
                .thenReturn(contentBytes);

        GDocEntity entity = mHelper.getGDocAsync("gdoc id", "text/html");
        assertThat(entity).isNotNull();
        assertThat(entity.getMetadata()).isSameAs(gDocMetadata);

        // Content is not retrieved yet
        verify(mGDocReader).getMetadataById("gdoc id");
        verify(mGDocReader, never()).readFileById(anyString(), anyString());

        // Store has not been updated yet
        verify(mHashStore, never()).putString(anyString(), anyString());
        verify(mBlobStore, never()).putBytes(anyString(), any());

        // Retrieve the content now, which triggers a fetch from gdoc
        assertThat(entity.getContent()).isEqualTo(contentBytes);

        verify(mGDocReader).readFileById("gdoc id", "text/html");

        // Store still has not been updated yet
        verify(mHashStore, never()).putString(anyString(), anyString());
        verify(mBlobStore, never()).putBytes(anyString(), any());

        // Sync to the store now.
        entity.syncToStore();

        // Update the store for both metadata and content
        verify(mHashStore).putString("gdoc-hash-gdoc id", "gdoc content hash");
        verify(mBlobStore).putBytes("gdoc-content-gdoc id-text/html", contentBytes);
    }
}
