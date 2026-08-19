// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*
import kotlin.test.*

// AFU's atomic properties should have a backing field

private val topLevelAI: AtomicInt get() = atomic(0)
private val topLevelAL: AtomicLong get() = atomic(0L)
private val topLevelAR: AtomicRef<Any?> get() = atomic("")
private val topLevelAB: AtomicBoolean get() = atomic(true)

private val topLevelAIA: AtomicIntArray get() = AtomicIntArray(1)
private val topLevelALA: AtomicLongArray get() = AtomicLongArray(1)
private val topLevelABA: AtomicBooleanArray get() = AtomicBooleanArray(1)
private val topLevelARA: AtomicArray<Any?> get() = atomicArrayOfNulls(1)

class Holder {
    private val ai: AtomicInt get() = atomic(0)
    private val al: AtomicLong get() = atomic(0L)
    private val ar: AtomicRef<Any?> get() = atomic("")
    private val ab: AtomicBoolean get() = atomic(true)

    private val aia: AtomicIntArray get() = AtomicIntArray(1)
    private val ala: AtomicLongArray get() = AtomicLongArray(1)
    private val aba: AtomicBooleanArray get() = AtomicBooleanArray(1)
    private val ara: AtomicArray<Any?> get() = atomicArrayOfNulls(1)
}
