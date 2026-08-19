// RENDER_DIAGNOSTICS_FULL_TEXT

@file:Suppress("NOTHING_TO_INLINE")

import kotlinx.atomicfu.*
import kotlin.test.*

// Non-inline and public extension functions are forbidden
public inline fun AtomicInt.f1() { }
@PublishedApi
internal inline fun AtomicRef<String>.f2() { }
private fun AtomicLong.f3() { }

// Passing atomics as parameters is prohibited
public fun f4(b: AtomicBoolean) { }
public inline fun f5(a: AtomicIntArray) { }
private fun f6(a: AtomicInt) { }
private inline fun f7(a: AtomicBooleanArray) { }

private inline fun AtomicInt.f8(a: AtomicInt) {}

public class X private constructor(private val a: AtomicInt)
