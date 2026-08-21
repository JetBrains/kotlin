// RENDER_DIAGNOSTICS_FULL_TEXT

@file:Suppress("NOTHING_TO_INLINE")

import kotlinx.atomicfu.*
import kotlin.test.*

// Non-inline and public extension functions are forbidden
public inline fun AtomicInt.<!ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE!>f1<!>() { }
@PublishedApi
internal inline fun AtomicRef<String>.<!ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE!>f2<!>() { }
private fun AtomicLong.<!ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE!>f3<!>() { }

// Passing atomics as parameters is prohibited
public fun f4(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>b<!>: AtomicBoolean) { }
public inline fun f5(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicIntArray) { }
private fun f6(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicInt) { }
private inline fun f7(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicBooleanArray) { }

private inline fun AtomicInt.f8(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicInt) {}

public class X private constructor(private val <!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicInt)
