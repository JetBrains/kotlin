// TODO(KT-65977): reenable these tests with caches
//IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
//IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
import kotlinx.atomicfu.*
import kotlin.test.*

class SimpleLockTest {
    fun withLock() {
        val lock = SimpleLock()
        val result = lock.withLock {
            "OK"
        }
        assertEquals("OK", result)
    }
}

class SimpleLock {
    private val _locked = atomic(0)

    fun <T> withLock(block: () -> T): T {
        // this contrieves construct triggers Kotlin compiler to reuse local variable slot #2 for
        // the exception in `finally` clause
        try {
            _locked.loop { locked ->
                check(locked == 0)
                if (!_locked.compareAndSet(0, 1)) return@loop // continue
                return block()
            }
        } finally {
            _locked.value = 0
        }
    }
}

@Test
fun box() {
    val testClass = SimpleLockTest()
    testClass.withLock()
}