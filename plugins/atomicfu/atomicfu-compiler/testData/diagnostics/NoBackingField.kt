// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*
import kotlin.test.*

// AFU's atomic properties should have a backing field

private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAI: AtomicInt get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0)<!>
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAL: AtomicLong get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0L)<!>
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAR: AtomicRef<Any?> get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic("")<!>
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAB: AtomicBoolean get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(true)<!>

private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelAIA: AtomicIntArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicIntArray(1)<!>
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelALA: AtomicLongArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicLongArray(1)<!>
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelABA: AtomicBooleanArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicBooleanArray(1)<!>
private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> topLevelARA: AtomicArray<Any?> get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomicArrayOfNulls(1)<!>

class Holder {
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ai: AtomicInt get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0)<!>
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> al: AtomicLong get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0L)<!>
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ar: AtomicRef<Any?> get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic("")<!>
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ab: AtomicBoolean get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(true)<!>

    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> aia: AtomicIntArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicIntArray(1)<!>
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ala: AtomicLongArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicLongArray(1)<!>
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> aba: AtomicBooleanArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicBooleanArray(1)<!>
    private <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> ara: AtomicArray<Any?> get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomicArrayOfNulls(1)<!>
}
