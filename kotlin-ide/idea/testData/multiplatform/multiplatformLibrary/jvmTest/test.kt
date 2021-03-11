package sample

import kotlin.test.Test
import kotlin.test.assertTrue

class <!LINE_MARKER("descr='Run Test'")!>SampleTestsJVM<!> {
    @Test
    fun <!LINE_MARKER("descr='Run Test'")!>testHello<!>() {
        assertTrue("JVM" in hello())
    }
}