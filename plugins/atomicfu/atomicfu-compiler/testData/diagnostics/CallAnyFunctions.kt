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
    println(<!ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY!>i.hashCode()<!>)
    println(<!ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY!>i.toString()<!>)
    println(<!ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY!>i.equals(null)<!>)

    println(<!ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY!>r.hashCode()<!>)
    println(<!ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY!>r.toString()<!>)
    println(<!ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY!>r.equals(null)<!>)

    println(<!ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY!>arr.hashCode()<!>)
    println(<!ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY!>arr.toString()<!>)
    println(<!ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY!>arr.equals(null)<!>)

    println(i.hashCode(13))
    println(r.toString("atomic"))
}
