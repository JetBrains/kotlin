// CHECK_HIGHLIGHTING
package verification.commonizer

import platform.Foundation.NSArgumentDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class SomethingInTest {
    @Test
    fun someTestInSharediOS() {
        val callingComm = NSArgumentDomain
        println(callingComm)
        assertEquals(42, 42)
    }
}
