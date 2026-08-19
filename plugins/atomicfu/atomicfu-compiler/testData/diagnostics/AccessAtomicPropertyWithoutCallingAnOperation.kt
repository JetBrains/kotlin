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
        println(tai)
        println(tal)
        println(tab)
        println(tar)
        println(taia)
        println(tala)
        println(taba)
        println(tara)

        println(ai)
        println(al)
        println(ab)
        println(ar)
        println(aia)
        println(ala)
        println(aba)
        println(ara)
    }

    @Suppress("UNCHECKED_CAST") fun castAndForget() {
        ar as AtomicRef<Any?>
        println(ar as AtomicRef<Any?>)
        // did not forget
        println((ar as AtomicRef<Any?>).value)
        // even though it's illegal
        println((ar as? AtomicRef<Any?>)?.value)
    }

    fun prohibitedAccesses() {
        ai
        sink = ai
        val local: Any = ai
        consume(ai)
    }

    fun atomicOperations() {
        ai.value
        ai.value = 1
        ai.compareAndSet(1, 2)
        aia[0].value
        aia[0].value = 1
    }
}
