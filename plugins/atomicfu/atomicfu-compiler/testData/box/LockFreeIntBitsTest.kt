import kotlinx.atomicfu.*
import kotlin.test.*

class LockFreeIntBitsTest {
    fun testBasic() {
        val bs = LockFreeIntBits()
        check(!bs[0])
        check(bs.bitSet(0))
        check(bs[0])
        check(!bs.bitSet(0))

        check(!bs[1])
        check(bs.bitSet(1))
        check(bs[1])
        check(!bs.bitSet(1))
        check(!bs.bitSet(0))

        check(bs[0])
        check(bs.bitClear(0))
        check(!bs.bitClear(0))

        check(bs[1])
    }
}

class LockFreeIntBits {
    private val bits = atomic(0)

    private fun Int.mask() = 1 shl this

    operator fun get(index: Int): Boolean = bits.value and index.mask() != 0

    // User-defined private inline function
    private inline fun bitUpdate(check: (Int) -> Boolean, upd: (Int) -> Int): Boolean {
        bits.update {
            if (check(it)) return false
            upd(it)
        }
        return true
    }

    fun bitSet(index: Int): Boolean {
        val mask = index.mask()
        return bitUpdate({ it and mask != 0 }, { it or mask })
    }

    fun bitClear(index: Int): Boolean {
        val mask = index.mask()
        return bitUpdate({ it and mask == 0 }, { it and mask.inv() })
    }
}

fun box(): String {
    val testClass = LockFreeIntBitsTest()
    testClass.testBasic()
    return "OK"
}