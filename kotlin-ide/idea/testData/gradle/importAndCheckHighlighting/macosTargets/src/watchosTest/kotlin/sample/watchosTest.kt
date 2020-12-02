package sample

import kotlin.test.Test
import kotlin.test.assertTrue

class <!LINE_MARKER{OSX}("descr='Run Test'")!>WatchOSTest<!> {

    @Test
    fun <!LINE_MARKER{OSX}("descr='Run Test'")!>testExample<!>() {
        assertTrue(Platform().name.contains("watchOS"), "Check watchOS is mentioned")
    }
}