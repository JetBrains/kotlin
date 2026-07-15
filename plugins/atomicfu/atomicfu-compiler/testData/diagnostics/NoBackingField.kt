// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*
import kotlin.test.*

private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAI: AtomicInt get() = atomic(0)
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAL: AtomicLong get() = atomic(0L)
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAR: AtomicRef<Any?> get() = atomic("")
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAB: AtomicBoolean get() = atomic(true)

private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAIA: AtomicIntArray get() = AtomicIntArray(1)
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelALA: AtomicLongArray get() = AtomicLongArray(1)
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelABA: AtomicBooleanArray get() = AtomicBooleanArray(1)
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelARA: AtomicArray<Any?> get() = atomicArrayOfNulls(1)

class Holder {
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ai: AtomicInt get() = atomic(0)
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> al: AtomicLong get() = atomic(0L)
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ar: AtomicRef<Any?> get() = atomic("")
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ab: AtomicBoolean get() = atomic(true)

    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> aia: AtomicIntArray get() = AtomicIntArray(1)
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ala: AtomicLongArray get() = AtomicLongArray(1)
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> aba: AtomicBooleanArray get() = AtomicBooleanArray(1)
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ara: AtomicArray<Any?> get() = atomicArrayOfNulls(1)
}
