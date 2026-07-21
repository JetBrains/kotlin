import kotlinx.atomicfu.*
import kotlin.test.*

class Delegator {
    private val a = atomic("")
    private var d: String by atomic("")

    private val b = atomic(0)
    private var bDelegate: Int by b

    init {
        d = "initialized"
        a.value = "initialized"
        bDelegate = -1
        b.value = 42
    }

    fun test() {
        assertEquals("initialized", d)
        assertEquals("initialized", a.value)
        assertEquals(42, b.value)
        assertEquals(42, bDelegate)
    }
}

fun box(): String {
    Delegator().test()
    return "OK"
}
