package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class ClassInheritanceTest extends TranslationTest {

    final private static String MAIN = "inheritance/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void initializersOfBasicClassExecute() throws Exception {
        testFunctionOutput("initializersOfBasicClassExecute.kt", "foo", "box", 3);
    }

    @Test
    public void methodOverride() throws Exception {
        testFooBoxIsTrue("methodOverride.kt");
    }

    @Test
    public void initializationOrder() throws Exception {
        testFooBoxIsTrue("initializationOrder.kt");
    }

    @Test
    public void complexInitializationOrder() throws Exception {
        testFooBoxIsTrue("complexInitializationOrder.kt");
    }

    @Test
    public void valuePassedToAncestorConstructor() throws Exception {
        testFooBoxIsTrue("valuePassedToAncestorConstructor.kt");
    }

    @Test
    public void baseClassDefinedAfterDerived() throws Exception {
        testFooBoxIsTrue("baseClassDefinedAfterDerived.kt");
    }

    @Test
    public void definitionOrder() throws Exception {
        testFooBoxIsTrue("definitionOrder.kt");
    }
}


