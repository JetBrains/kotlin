package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class NameClashesTest extends TranslationTest {

    private static final String MAIN = "nameClashes/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void methodOverload() throws Exception {
        testFooBoxIsTrue("methodOverload.kt");
    }
}