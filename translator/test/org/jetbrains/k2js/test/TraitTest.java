package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class TraitTest extends AbstractClassTest {

    final private static String MAIN = "trait/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void traitCompiles() throws Exception {
        testFooBoxIsTrue("traitCompiles.kt");
    }

    @Test
    public void traitAddsFunctionsToClass() throws Exception {
        testFooBoxIsTrue("traitAddsFunctionsToClass.kt");
    }
}
