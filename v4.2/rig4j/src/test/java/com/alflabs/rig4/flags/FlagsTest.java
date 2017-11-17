package com.alflabs.rig4.flags;

import com.alflabs.utils.FileOps;
import com.alflabs.utils.StringLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Properties;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

/** Test for {@link Flags}. */
public class FlagsTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock FileOps mFileOps;

    private final StringLogger mLogger = new StringLogger();
    private Flags mFlags;

    @Before
    public void setUp() throws Exception {
        mFlags = new Flags(mFileOps, mLogger);
        mFlags.addBool("help", false, "Display help and usage");
        mFlags.addInt("answer", 42, "The answer");
        mFlags.addString("username", "joe", null);
    }

    @Test
    public void testUsage() throws Exception {
        mFlags.usage();
        assertThat(mLogger.getString()).isEqualTo("" +
                "Usage: --answer  : 42   , The answer\n" +
                "Usage: --help    : false, Display help and usage\n" +
                "Usage: --username: joe  \n");
    }

    @Test
    public void testDefaults() throws Exception {
        assertThat(mFlags.getBool("help")).isFalse();
        assertThat(mFlags.getInt("answer")).isEqualTo(42);
        assertThat(mFlags.getString("username")).isEqualTo("joe");
        assertThat(mLogger.getString()).isEmpty();
    }

    @Test
    public void testCommandLine() throws Exception {
        mFlags.parseCommandLine(new String[] {
                "-h",
                "--answer=314",
                "-u",
                "bar"
        });
        assertThat(mFlags.getBool("help")).isTrue();
        assertThat(mFlags.getInt("answer")).isEqualTo(314);
        assertThat(mFlags.getString("username")).isEqualTo("bar");
        assertThat(mLogger.getString()).isEmpty();
    }

    @Test
    public void testCommandLine_UnknownParameter() throws Exception {
        mFlags.parseCommandLine(new String[] {
                "--show-help",
                "--a",
                "314",
                "-username=bar"
        });
        assertThat(mFlags.getBool("help")).isFalse();
        assertThat(mFlags.getInt("answer")).isEqualTo(314);
        assertThat(mFlags.getString("username")).isEqualTo("bar");
        assertThat(mLogger.getString()).isEqualTo("Flags: Unknown parameter: --show-help\n");
    }

    @Test
    public void testParseConfigFile_NoSuchFile() throws Exception {
        mFlags.parseConfigFile("/example/config");
        assertThat(mLogger.getString()).isEqualTo("Flags: No config file '/example/config'\n");
    }

    @Test
    public void testParseConfigFile() throws Exception {
        File file = new File("/example/config");
        Properties props = new Properties();
        props.setProperty("help", "true");
        props.setProperty("username", "bar");


        when(mFileOps.isFile(file)).thenReturn(true);
        when(mFileOps.getProperties(file)).thenReturn(props);

        mFlags.parseConfigFile("/example/config");
        assertThat(mFlags.getBool("help")).isTrue();
        assertThat(mFlags.getInt("answer")).isEqualTo(42);
        assertThat(mFlags.getString("username")).isEqualTo("bar");
        assertThat(mLogger.getString()).isEmpty();
    }
}
