package sample

import kotlin.test.Test
import kotlin.test.assertTrue

class <!LINE_MARKER("descr='Run Test'")!>SampleTests<!> {
    @Test
    fun <!LINE_MARKER("descr='Run Test'")!>testMe<!>() {
        assertTrue(Sample().checkMe() > 0)
    }
}