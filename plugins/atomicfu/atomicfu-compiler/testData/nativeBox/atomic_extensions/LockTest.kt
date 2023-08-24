import kotlinx.atomicfu.*
import kotlin.test.*

class LockTest {
    private val inProgressLock = atomic(false)

    fun testLock() {
        var result = ""
        if (inProgressLock.tryAcquire()) {
            result = "OK"
        }
        assertEquals("OK", result)
    }
}

// This function will be removed by transformer
@Suppress("NOTHING_TO_INLINE")
private inline fun AtomicBoolean.tryAcquire(): Boolean = compareAndSet(false, true)

// This function is here to test if the Kotlin metadata still consistent after transform
// It is used in ReflectionTest, DO NOT REMOVE
@Suppress("UNUSED_PARAMETER")
fun <AA, BB : Number> String.reflectionTest(mapParam: Map<in AA, BB>): List<BB> = error("no impl")

@Test
fun box() {
    val testClass = LockTest()
    testClass.testLock()
}