package com.alflabs.rig4;

import com.alflabs.utils.IClock;
import com.alflabs.utils.MockClock;
import com.alflabs.utils.StringLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.truth.Truth.assertThat;

public class TimingTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    private StringLogger mLogger = new StringLogger();
    private IClock mClock = new MockClock();
    private Timing mTiming;

    @Before
    public void setUp() throws Exception {
        mTiming = new Timing(mClock, mLogger);
    }

    @Test
    public void testTiming() throws Exception {
        Timing.TimeAccumulator acc1 = mTiming.get("name1");
        Timing.TimeAccumulator acc2 = mTiming.get("name2");

        acc1.start();
        mClock.sleep(101);;

        acc2.start();
        mClock.sleep(901);
        acc2.end();

        acc1.end();

        assertThat(acc1.getName()).isEqualTo("name1");
        assertThat(acc1.getAccumulator()).isEqualTo(101 + 901);

        assertThat(acc2.getName()).isEqualTo("name2");
        assertThat(acc2.getAccumulator()).isEqualTo(901);

        mTiming.printToLog();

        assertThat(mLogger.getString()).isEqualTo(
                "Timing: name1 = 1.002 s\n" +
                "Timing: name2 = 0.901 s\n");
    }
}
