import kotlinx.atomicfu.locks.*
import kotlin.test.*

//class ReentrantLockTest {
//    private val lock = reentrantLock()
//    private var state = 0
//
//    fun testLockField() {
//        lock.withLock {
//            state = 1
//        }
//        assertEquals(1, state)
//    }
//}

class ReentrantLockFieldTest {
    private val lock = reentrantLock()
    private var state = 0

    fun testLock() {
        lock.withLock {
            state = 1
        }
        assertEquals(1, state)
        assertTrue(lock.tryLock())
        lock.unlock()
    }
}

fun box(): String {
    val testClass = ReentrantLockFieldTest()
    testClass.testLock()
    return "OK"
}