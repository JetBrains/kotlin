// RENDER_DIAGNOSTICS_FULL_TEXT

// Nullable AFU's atomic properties should be forbidden

import kotlinx.atomicfu.*

private val tai: AtomicInt? = null
private val tal: AtomicLong? = null
private val tab: AtomicBoolean? = null
private val tar: AtomicRef<Any>? = null

private val taia: AtomicIntArray? = null
private val tala: AtomicLongArray? = null
private val taba: AtomicBooleanArray? = null
private val tara: AtomicArray<Any>? = null

class C {
    private val ai: AtomicInt? = null
    private val al: AtomicLong? = null
    private val ab: AtomicBoolean? = null
    private val ar: AtomicRef<Any>? = null

    private val aia: AtomicIntArray? = null
    private val ala: AtomicLongArray? = null
    private val aba: AtomicBooleanArray? = null
    private val ara: AtomicArray<Any>? = null
}
