package sample

import kotlin.test.Test
import kotlin.test.assertTrue

class <!LINE_MARKER("descr='Run Test'")!>CommonTest<!> {

    @Test
    fun <!LINE_MARKER("descr='Run Test'")!>testExample<!>() {
        assertTrue(Platform().name.isNotEmpty())
    }
}