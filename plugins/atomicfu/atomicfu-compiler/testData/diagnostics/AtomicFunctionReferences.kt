// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*
import kotlin.test.*

fun callAndPrint(f: () -> Int) = println(f())
private <!NOTHING_TO_INLINE!>inline<!> fun AtomicInt.inc() = this.incrementAndGet()
private val a: AtomicInt = atomic(42)

fun whyWouldYouDoThat() {
   val inc = a::<!ATOMIC_FUNCTION_CALLABLE_REFERENCES_ARE_FORBIDDEN!>incrementAndGet<!>
   callAndPrint(inc)
   val inc2 = a::<!ATOMIC_FUNCTION_CALLABLE_REFERENCES_ARE_FORBIDDEN!>inc<!>
   callAndPrint(inc)
}
