// DUMP_IR_DIFFERENCE: JS_IR, NATIVE
import kotlinx.atomicfu.*
import kotlin.test.*

class Holder {
    internal val state = atomic(0)
}

fun Holder.advance(): Int = state.incrementAndGet()
fun Holder.fastForward(): Int = state.updateAndGet { it + 10 }
val Holder.currentState: Int get() = state.value

fun test() {
    val holder = Holder()
    assertEquals(0, holder.currentState)
    assertEquals(1, holder.advance())
    assertEquals(1, holder.currentState)
    assertEquals(11, holder.fastForward())
    assertEquals(11, holder.currentState)
}

fun box(): String {
    test()
    return "OK"
}
