package org.jetbrains.k2js.test;

/**
 * @author Talanov Pavel
 */
public class TupleTest extends IncludeLibraryTest {

    final private static String MAIN = "tuple/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    //TODO: excluded because Tuples are not implemented
//    @Test
//    public void twoElements() throws Exception {
//        testFooBoxIsTrue("twoElements.kt");
//    }

}

