import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic

internal class Seg

internal class Head {
    val tail = atomic(Seg())
    private fun doNothing() {}

    fun bogus() {
        tail.find(::doNothing)
    }
}
@Suppress("NOTHING_TO_INLINE")
internal inline fun AtomicRef<Seg>.find(noinline callback: () -> Unit) {
    findInternal(callback)
}
internal fun findInternal(callback: () -> Unit): Unit {}

fun box(): String {
    Head().bogus()
    return "OK"
}
