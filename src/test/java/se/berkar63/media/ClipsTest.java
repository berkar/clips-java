package se.berkar63.media;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for media App.
 */
public class ClipsTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ClipsTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ClipsTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testClips() throws Exception {
        Clips.run("src/test/init.txt");
    }
}
