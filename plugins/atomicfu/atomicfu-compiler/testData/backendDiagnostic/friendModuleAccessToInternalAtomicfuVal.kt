// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// DISABLE_IR_VISIBILITY_CHECKS: NATIVE
// ^^^ Because AtomicFU plugin generates an IR property reference node that refers to a private property, KT-85180.

// MODULE: lib
// FILE: lib.kt
import kotlinx.atomicfu.*

internal val a = atomic(0)

internal fun set() {
    a.value = 1
}

inline internal fun compareAndSet(expected: Int, newValue: Int): Boolean =
    a.compareAndSet(expected, newValue)

class C {
    internal val a = atomic(0)

    internal fun getAndSet(newValue: Int): Int =
        a.getAndSet(newValue)

    inline internal fun getAndAdd(delta: Int): Int =
        a.getAndAdd(delta)
}

// MODULE: test()(lib)
// FILE: test.kt
import kotlinx.atomicfu.*

fun box(): String {
    <!LEAKED_VOLATILE_FIELD!>a<!>.value
    set()
    compareAndSet(1, 2)

    val c = C()
    c.<!LEAKED_VOLATILE_FIELD!>a<!>.value
    c.getAndSet(1)
    c.getAndAdd(2)

    return "OK"
}
