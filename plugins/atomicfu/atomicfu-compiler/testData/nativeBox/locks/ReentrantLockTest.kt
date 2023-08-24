import kotlinx.atomicfu.locks.*
import kotlin.test.*

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

@Test
fun box() {
    val testClass = ReentrantLockFieldTest()
    testClass.testLock()
}