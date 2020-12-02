package sample

import kotlin.test.Test
import kotlin.test.assertTrue

class <!LINE_MARKER{OSX}("descr='Run Test'")!>IOSTest<!> {

    @Test
    fun <!LINE_MARKER{OSX}("descr='Run Test'")!>testExample<!>() {
        assertTrue(Platform().name.contains("iOS"), "Check iOS is mentioned")
    }
}