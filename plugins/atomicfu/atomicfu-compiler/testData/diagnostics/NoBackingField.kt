// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*
import kotlin.test.*

// AFU's atomic properties should have a backing field

private val topLevelAI: AtomicInt get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0)<!>
private val topLevelAL: AtomicLong get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0L)<!>
private val topLevelAR: AtomicRef<Any?> get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic("")<!>
private val topLevelAB: AtomicBoolean get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(true)<!>

private val topLevelAIA: AtomicIntArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicIntArray(1)<!>
private val topLevelALA: AtomicLongArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicLongArray(1)<!>
private val topLevelABA: AtomicBooleanArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicBooleanArray(1)<!>
private val topLevelARA: AtomicArray<Any?> get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomicArrayOfNulls(1)<!>

class Holder {
    private val ai: AtomicInt get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0)<!>
    private val al: AtomicLong get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0L)<!>
    private val ar: AtomicRef<Any?> get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic("")<!>
    private val ab: AtomicBoolean get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(true)<!>

    private val aia: AtomicIntArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicIntArray(1)<!>
    private val ala: AtomicLongArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicLongArray(1)<!>
    private val aba: AtomicBooleanArray get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicBooleanArray(1)<!>
    private val ara: AtomicArray<Any?> get() = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomicArrayOfNulls(1)<!>
}
