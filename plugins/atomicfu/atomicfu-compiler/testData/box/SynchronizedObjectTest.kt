import kotlinx.atomicfu.locks.*
import kotlin.test.*

class SynchronizedObjectTest : SynchronizedObject() {

    fun testSync() {
        val result = synchronized(this) { bar() }
        assertEquals("OK", result)
    }

    private fun bar(): String =
        synchronized(this) {
            "OK"
        }
}

fun box(): String {
    val testClass = SynchronizedObjectTest()
    testClass.testSync()
    return "OK"
}