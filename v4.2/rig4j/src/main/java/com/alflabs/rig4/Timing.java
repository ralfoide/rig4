package com.alflabs.rig4;

import com.alflabs.annotations.NonNull;
import com.alflabs.utils.IClock;
import com.alflabs.utils.ILogger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.TreeMap;

@Singleton
public class Timing {
    private final IClock mClock;
    private final ILogger mLogger;
    private Map<String, TimeAccumulator> mMap = new TreeMap<>();

    @Inject
    public Timing(IClock clock, ILogger logger) {
        mClock = clock;
        mLogger = logger;
    }

    public TimeAccumulator get(@NonNull String name) {
        TimeAccumulator a = mMap.get(name);
        if (a == null) {
            a = new TimeAccumulator(name);
            mMap.put(name, a);
        }
        return a;
    }

    public void printToLog() {
        for (Map.Entry<String, TimeAccumulator> entry : mMap.entrySet()) {
            mLogger.d("Timing",
                    String.format("%s = %.3f s", entry.getKey(),
                            entry.getValue().getAccumulator() / 1000.0f));
        }
    }

    public class TimeAccumulator {
        private final String mName;
        private long mStart;
        private long mAccumulator;

        public TimeAccumulator(@NonNull String name) {
            mName = name;
        }

        public TimeAccumulator start() {
            mStart = mClock.elapsedRealtime();
            return this;
        }

        public void end() {
            if (mStart > 0) {
                long delay = mClock.elapsedRealtime() - mStart;
                mAccumulator += delay;
                mStart = 0;
            }
        }

        public String getName() {
            return mName;
        }

        public long getAccumulator() {
            return mAccumulator;
        }
    }
}
