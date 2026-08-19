// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*

private val i = atomic(0)
private val r = atomic("")
private val arr = AtomicIntArray(1)

@Suppress("NOTHING_TO_INLINE")
private inline fun AtomicInt.hashCode(seed: Int): Int = seed xor value
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> AtomicRef<T>.toString(prefix: String): String = "prefix: $value"

fun test() {
    println(i.hashCode())
    println(i.toString())
    println(i.equals(null))

    println(r.hashCode())
    println(r.toString())
    println(r.equals(null))

    println(arr.hashCode())
    println(arr.toString())
    println(arr.equals(null))

    println(i.hashCode(13))
}
