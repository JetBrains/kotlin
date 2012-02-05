package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class ClassInheritanceTest extends TranslationTest {

    final private static String MAIN = "inheritance/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testInitializersOfBasicClassExecute() throws Exception {
        testFunctionOutput("initializersOfBasicClassExecute.kt", "foo", "box", 3);
    }

    public void testMethodOverride() throws Exception {
        testFooBoxIsTrue("methodOverride.kt");
    }

    public void testInitializationOrder() throws Exception {
        testFooBoxIsTrue("initializationOrder.kt");
    }

    public void testComplexInitializationOrder() throws Exception {
        testFooBoxIsTrue("complexInitializationOrder.kt");
    }

    public void testValuePassedToAncestorConstructor() throws Exception {
        testFooBoxIsTrue("valuePassedToAncestorConstructor.kt");
    }

    public void testBaseClassDefinedAfterDerived() throws Exception {
        testFooBoxIsTrue("baseClassDefinedAfterDerived.kt");
    }

    public void testDefinitionOrder() throws Exception {
        testFooBoxIsTrue("definitionOrder.kt");
    }
}


