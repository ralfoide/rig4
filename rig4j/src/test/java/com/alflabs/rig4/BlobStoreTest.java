package com.alflabs.rig4;

import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.FakeFileOps;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.MockClock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.truth.Truth.assertThat;

public class BlobStoreTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private Flags mFlags;
    @Mock private ILogger mLogger;
    private final Timing mTiming = new Timing(new MockClock(), mLogger);

    private FileOps mFileOps;
    private BlobStore mStore;


    @Before
    public void setUp() throws Exception {
        mFileOps = new FakeFileOps();
        mStore = new BlobStore(mFlags, mFileOps, mTiming, mLogger);
    }

    @Test
    public void testString() throws Exception {
        assertThat(mStore.getString("key")).isNull();

        mStore.putString("key", "content");
        assertThat(mStore.getString("key")).isEqualTo("content");
    }

    @Test
    public void testBytes() throws Exception {
        byte[] actual = new byte[]{1, 2, 3, 4};;
        assertThat(mStore.getBytes("key")).isNull();

        mStore.putBytes("key", actual);
        assertThat(mStore.getBytes("key")).isEqualTo(actual);
    }

    @Test
    public void testJson() throws Exception {
        JsonStruct actual = new JsonStruct("The answer is", 42);
        assertThat(mStore.getJson("key", JsonStruct.class)).isNull();

        mStore.putJson("key", actual);
        assertThat(mStore.getJson("key", JsonStruct.class)).isEqualTo(actual);
    }

    public static class JsonStruct {
        public String fieldA;
        public int fieldB;

        // Jackson can create an instance using the default constructor and setting the fields.
        public JsonStruct() {}

        public JsonStruct(String fieldA, int fieldB) {
            this.fieldA = fieldA;
            this.fieldB = fieldB;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JsonStruct that = (JsonStruct) o;

            if (fieldB != that.fieldB) return false;
            return fieldA != null ? fieldA.equals(that.fieldA) : that.fieldA == null;
        }

        @Override
        public int hashCode() {
            int result = fieldA != null ? fieldA.hashCode() : 0;
            result = 31 * result + fieldB;
            return result;
        }
    }
}
