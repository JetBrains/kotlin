import kotlinx.atomicfu.locks.*
import kotlin.test.*

class ReentrantLockTest {
    private val lock = reentrantLock()
    private var state = 0

    fun testLockField() {
        lock.withLock {
            state = 1
        }
        assertEquals(1, state)
    }
}

fun box(): String {
    val testClass = ReentrantLockTest()
    testClass.testLockField()
    return "OK"
}