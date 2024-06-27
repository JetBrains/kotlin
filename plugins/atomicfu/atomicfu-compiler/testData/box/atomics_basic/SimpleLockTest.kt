import kotlinx.atomicfu.*
import kotlin.test.*

class SimpleLockTest {
    fun withLock() {
        val lock = SimpleLock()
        val res = lock.withLock("OK")
        assertEquals("OK", res)
    }
}

class SimpleLock {
    private val _locked = atomic(0)

    fun <T> withLock(res: T): T {
        try {
            _locked.loop { locked ->
                check(locked == 0)
                if (!_locked.compareAndSet(0, 1)) return@loop // continue
                return res
            }
        } finally {
            _locked.value = 0
        }
    }
}

fun box(): String {
    val testClass = SimpleLockTest()
    testClass.withLock()
    return "OK"
}