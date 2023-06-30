package com.alflabs.rig4k.common

import com.alflabs.utils.FakeFileOps
import com.alflabs.utils.ILogger
import com.alflabs.utils.MockClock
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

class BlobStoreTest {
    @get:Rule var mockitoRule: MockitoRule = MockitoJUnit.rule()

    private val logger : ILogger = mock()
    private val options : BlobStoreOptions = mock()
    private val timing = Timing(MockClock(), logger)
    private val fileOps = FakeFileOps()
    private lateinit var mStore: BlobStore

    @Before
    fun setUp() {
        mStore = BlobStore(options, fileOps, logger, timing)
    }

    @Test
    fun testString() {
        Truth.assertThat(mStore.getString("key")).isNull()
        mStore.putString("key", "content")
        Truth.assertThat(mStore.getString("key")).isEqualTo("content")
    }

    @Test
    fun testBytes() {
        val actual = byteArrayOf(1, 2, 3, 4)
        Truth.assertThat(mStore.getBytes("key")).isNull()
        mStore.putBytes("key", actual)
        Truth.assertThat(mStore.getBytes("key")).isEqualTo(actual)
    }

    @Test
    fun testJson() {
        val actual = JsonStruct("The answer is", 42)
        Truth.assertThat(mStore.getJson("key", JsonStruct::class.java)).isNull()
        mStore.putJson("key", actual)
        Truth.assertThat(mStore.getJson("key", JsonStruct::class.java)).isEqualTo(actual)
    }

    class JsonStruct {
        var fieldA: String? = null
        var fieldB = 0

        // Jackson can create an instance using the default constructor and setting the fields.
        constructor()

        constructor(fieldA: String?, fieldB: Int) {
            this.fieldA = fieldA
            this.fieldB = fieldB
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as JsonStruct
            if (fieldB != that.fieldB) return false
            return if (fieldA != null) fieldA == that.fieldA else that.fieldA == null
        }

        override fun hashCode(): Int {
            var result = if (fieldA != null) fieldA.hashCode() else 0
            result = 31 * result + fieldB
            return result
        }
    }
}
