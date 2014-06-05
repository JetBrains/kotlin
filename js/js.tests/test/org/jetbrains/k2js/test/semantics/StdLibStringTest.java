package org.jetbrains.k2js.test.semantics;

import junit.framework.Test;

@SuppressWarnings("JUnitTestCaseWithNoTests")
public final class StdLibStringTest extends JsUnitTestBase {
    public static Test suite() throws Exception {
        return createTestSuiteForFile("libraries/stdlib/test/text/StringTest.kt");
    }
}
