package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class StdlibTest extends TranslationTest {
    final private static String MAIN = "stdlib/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    //TODO: cant do due to big java dependencies
//    @Test
//    public void filter() throws Exception {
//         testFooBoxIsTrue("Filter.kt");
//    }
}
