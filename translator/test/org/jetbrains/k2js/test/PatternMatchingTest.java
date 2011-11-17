package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class PatternMatchingTest extends IncludeLibraryTest {

    final private static String MAIN = "patternMatching/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void whenType() throws Exception {
        testFooBoxIsTrue("whenType.kt");
    }

    @Test
    public void whenNotType() throws Exception {
        testFooBoxIsTrue("whenNotType.kt");
    }

}
