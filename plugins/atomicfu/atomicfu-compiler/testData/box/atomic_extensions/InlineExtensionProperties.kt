import kotlinx.atomicfu.*
import kotlin.test.*
import kotlin.math.abs

private inline val AtomicInt.abs: Int
    get() = abs(value)

private inline val AtomicRef<String>.isEmpty: Boolean
    get() = value.isEmpty()

private inline var AtomicInt.scaledBy10: Int
    get() = value * 10
    set(value) {
        this.value = value / 10
    }

private inline val AtomicLong.low: Int
    get() = value.toInt()

private inline val AtomicBoolean.isTrue: Boolean
    get() = value

private val a = atomic(0)
private val s = atomic("TEST")
private val arr = AtomicIntArray(10)
private val l = atomic(0x7fff_ffff_0000_0001L)
private val b = atomic(false)

class C {
    private val ma = atomic(0)
    private val ms = atomic("TEST")
    private val marr = AtomicIntArray(10)
    private val ml = atomic(0x7fff_ffff_0000_0001L)
    private val mb = atomic(false)

    private inline val AtomicInt.squared: Int get() = value * value

    fun memberTest() {
        ma.value = -10
        assertEquals(10, ma.abs)
        assertFalse(ms.isEmpty)
        assertEquals(-100, ma.scaledBy10)
        ma.scaledBy10 = 200
        assertEquals(20, ma.value)
        marr[0].scaledBy10 = 300
        assertEquals(30, marr[0].value)
        assertFalse(mb.isTrue)
        assertEquals(1, ml.low)

        assertEquals(400, ma.squared)
    }
}

fun topLevelTest() {
    a.value = -10
    assertEquals(10, a.abs)
    assertFalse(s.isEmpty)
    assertEquals(-100, a.scaledBy10)
    a.scaledBy10 = 200
    assertEquals(20, a.value)
    arr[0].scaledBy10 = 300
    assertEquals(30, arr[0].value)
    assertFalse(b.isTrue)
    assertEquals(1, l.low)
}

fun box(): String {
    topLevelTest()
    C().memberTest()
    return "OK"
}
