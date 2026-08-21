// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*

private val tai: AtomicInt = atomic(0)
private val tal: AtomicLong = atomic(0L)
private val tab: AtomicBoolean = atomic(false)
private val tar: AtomicRef<Any> = atomic("")

private val taia: AtomicIntArray = AtomicIntArray(1)
private val tala: AtomicLongArray = AtomicLongArray(1)
private val taba: AtomicBooleanArray = AtomicBooleanArray(1)
private val tara: AtomicArray<Any?> = atomicArrayOfNulls(1)

private var sink: Any? = null

private fun consume(value: Any?) {}

class C {
    private val ai: AtomicInt = atomic(0)
    private val al: AtomicLong = atomic(0L)
    private val ab: AtomicBoolean = atomic(false)
    private val ar: AtomicRef<Any> = atomic("")

    private val aia: AtomicIntArray = AtomicIntArray(1)
    private val ala: AtomicLongArray = AtomicLongArray(1)
    private val aba: AtomicBooleanArray = AtomicBooleanArray(1)
    private val ara: AtomicArray<Any?> = atomicArrayOfNulls(1)

    fun printAll() {
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>tai<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>tal<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>tab<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>tar<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>taia<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>tala<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>taba<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>tara<!>)

        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ai<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>al<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ab<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ar<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>aia<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ala<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>aba<!>)
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ara<!>)
    }

    @Suppress("UNCHECKED_CAST") fun castAndForget() {
        <!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ar<!> as AtomicRef<Any?>
        println(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ar<!> as AtomicRef<Any?>)
        // did not forget
        println((ar as AtomicRef<Any?>).value)
        // even though it's illegal
        println((<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>ar as? AtomicRef<Any?><!>)?.value)
    }

    fun prohibitedAccesses() {
        <!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ai<!>
        sink = <!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ai<!>
        val local: Any = <!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ai<!>
        consume(<!ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION!>ai<!>)
    }

    fun atomicOperations() {
        ai.value
        ai.value = 1
        ai.compareAndSet(1, 2)
        aia[0].value
        aia[0].value = 1
    }
}
