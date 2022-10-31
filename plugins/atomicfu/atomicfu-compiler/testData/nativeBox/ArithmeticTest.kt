// FREE_COMPILER_ARGS: -Xplugin=plugins/atomicfu/atomicfu-compiler/build/libs/kotlinx-atomicfu-compiler-plugin-1.8.255-SNAPSHOT.jar

import kotlinx.atomicfu.*
import kotlin.test.*

class IntArithmetic {
    val x = atomic(0)
}

class ArithmeticTest {

    fun testInt() {
        val a = IntArithmetic()
        assertTrue(a.x.compareAndSet(0, 3))
        assertEquals(5, a.x.addAndGet(3))
    }
}

fun box(): String {
    val testClass = ArithmeticTest()
    testClass.testInt()
    return "OK"
}
