package com.alflabs.rig4k.common

import com.alflabs.utils.IClock
import com.alflabs.utils.MockClock
import com.alflabs.utils.StringLogger
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

class TimingTest {
    @get:Rule var mockitoRule: MockitoRule = MockitoJUnit.rule()
    private val logger = StringLogger()
    private val clock: IClock = MockClock()

    private lateinit var timing: Timing

    @Before
    fun setUp() {
        timing = Timing(clock, logger)
    }

    @Test
    fun testTiming() {
        val acc1 = timing.get("name1")
        val acc2 = timing.get("name2")
        acc1.start()
        clock.sleep(101)
        acc2.start()
        clock.sleep(901)
        acc2.end()
        acc1.end()
        assertThat(acc1.name).isEqualTo("name1")
        assertThat(acc1.accumulator).isEqualTo(101 + 901)
        assertThat(acc2.name).isEqualTo("name2")
        assertThat(acc2.accumulator).isEqualTo(901)
        timing.printToLog()
        assertThat(logger.string).isEqualTo(
            """Timing: name1 = 1.002 s
              |Timing: name2 = 0.901 s
              |""".trimMargin()
        )
    }
}
