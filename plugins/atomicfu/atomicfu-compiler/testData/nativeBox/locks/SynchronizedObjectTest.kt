// TODO(KT-65977): reenable these tests with caches
//IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
//IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
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

@Test
fun box() {
    val testClass = SynchronizedObjectTest()
    testClass.testSync()
}