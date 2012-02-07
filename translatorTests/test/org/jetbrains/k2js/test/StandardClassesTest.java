package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public class StandardClassesTest extends TranslationTest {
    final private static String MAIN = "standardClasses/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testArray() throws Exception {
        testFooBoxIsTrue("array.kt");
    }


    public void testArrayAccess() throws Exception {
        testFooBoxIsTrue("arrayAccess.kt");
    }


    public void testArrayIsFilledWithNulls() throws Exception {
        testFooBoxIsTrue("arrayIsFilledWithNulls.kt");
    }


    public void testArrayFunctionConstructor() throws Exception {
        testFooBoxIsTrue("arrayFunctionConstructor.kt");
    }


    public void testArraySize() throws Exception {
        testFooBoxIsTrue("arraySize.kt");
    }

    //TODO: this feature in not supported for some time
    //TODO: support it. Probably configurable.
//    (expected = JavaScriptException.class)
//    public void arrayThrowsExceptionOnOOBaccess() throws Exception {
//        testFooBoxIsTrue("arrayThrowsExceptionOnOOBaccess.kt");
//    }


    public void testArraysIterator() throws Exception {
        testFooBoxIsTrue("arraysIterator.kt");
    }
}
