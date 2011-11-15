package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class ClassInheritanceTest extends AbstractClassTest {

    final private static String MAIN = "inheritance/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void initializersOfBasicClassExecute() throws Exception {
        performTest("initializersOfBasicClassExecute.kt", "foo", "box", 3);
    }

    @Test
    public void methodOverride() throws Exception {
        testFooBoxIsTrue("methodOverride.kt");
    }

    @Test
    public void initializationOrder() throws Exception {
        testFooBoxIsTrue("initializationOrder.kt");
    }
}


