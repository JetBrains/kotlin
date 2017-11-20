@file:kotlin.jvm.JvmVersion
package test.collections

import kotlin.test.*
import java.util.*

class IteratorsJVMTest {

    @Test fun testEnumeration() {
        val v = Vector<Int>()
        for (i in 1..5)
            v.add(i)

        var sum = 0
        for (k in v.elements())
            sum += k

        assertEquals(15, sum)
    }
}
