package com.alflabs.rig4k.common

import com.alflabs.utils.IClock
import com.alflabs.utils.ILogger
import java.util.TreeMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Timing @Inject constructor(
    private val clock: IClock,
    private val logger: ILogger
) {
    private val map: MutableMap<String, TimeAccumulator> = TreeMap()

    fun get(name: String): TimeAccumulator {
        var a = map[name]
        if (a == null) {
            a = TimeAccumulator(name)
            map[name] = a
        }
        return a
    }

    fun printToLog() {
        for ((key, value) in map) {
            logger.d(
                "Timing", String.format(
                    "%s = %.3f s", key,
                    value.accumulator / 1000.0f
                )
            )
        }
    }

    inner class TimeAccumulator(val name: String) {
        private var start: Long = 0
        var accumulator: Long = 0
            private set

        fun start(): TimeAccumulator {
            start = clock.elapsedRealtime()
            return this
        }

        fun end() {
            if (start > 0) {
                val delay = clock.elapsedRealtime() - start
                accumulator += delay
                start = 0
            }
        }
    }
}
