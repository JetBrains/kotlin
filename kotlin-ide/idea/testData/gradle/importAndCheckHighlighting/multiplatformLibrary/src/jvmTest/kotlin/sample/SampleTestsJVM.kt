package sample

import kotlin.test.Test
import kotlin.test.assertTrue

class <lineMarker descr="Run Test">SampleTestsJVM</lineMarker> {
    @Test
    fun <lineMarker descr="Run Test">testHello</lineMarker>() {
        assertTrue("JVM" in hello())
    }
}