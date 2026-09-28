// RENDER_DIAGNOSTICS_FULL_TEXT

@file:Suppress("NOTHING_TO_INLINE")

import kotlinx.atomicfu.*
import kotlin.test.*

fun callAndPrint(f: () -> Int) = println(f())
private inline fun AtomicInt.inc() = this.incrementAndGet()
private val a: AtomicInt = atomic(42)

// Getting a reference to atomic function is prohibited
fun whyWouldYouDoThat() {
   val inc = a::<!ATOMIC_FUNCTION_CALLABLE_REFERENCES_ARE_FORBIDDEN!>incrementAndGet<!>
   callAndPrint(inc)
   val inc2 = a::<!ATOMIC_FUNCTION_CALLABLE_REFERENCES_ARE_FORBIDDEN!>inc<!>
   callAndPrint(inc)
}
