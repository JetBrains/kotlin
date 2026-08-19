// RENDER_DIAGNOSTICS_FULL_TEXT

// Nullable AFU's atomic properties should be forbidden

import kotlinx.atomicfu.*

private val tai: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicInt?<!> = null
private val tal: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicLong?<!> = null
private val tab: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicBoolean?<!> = null
private val tar: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicRef<Any>?<!> = null

private val taia: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicIntArray?<!> = null
private val tala: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicLongArray?<!> = null
private val taba: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicBooleanArray?<!> = null
private val tara: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicArray<Any>?<!> = null

class C {
    private val ai: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicInt?<!> = null
    private val al: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicLong?<!> = null
    private val ab: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicBoolean?<!> = null
    private val ar: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicRef<Any>?<!> = null

    private val aia: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicIntArray?<!> = null
    private val ala: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicLongArray?<!> = null
    private val aba: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicBooleanArray?<!> = null
    private val ara: <!NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN!>AtomicArray<Any>?<!> = null
}
