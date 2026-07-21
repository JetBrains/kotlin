// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*
import kotlin.test.*

public <!NOTHING_TO_INLINE!>inline<!> fun AtomicInt.<!ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE!>f1<!>() { }
@PublishedApi
internal <!NOTHING_TO_INLINE!>inline<!> fun AtomicRef<String>.<!ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE!>f2<!>() { }
private fun AtomicLong.<!ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE!>f3<!>() { }

public fun f4(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>b<!>: AtomicBoolean) { }
public <!NOTHING_TO_INLINE!>inline<!> fun f5(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicIntArray) { }
private fun f6(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicInt) { }
private <!NOTHING_TO_INLINE!>inline<!> fun f7(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicBooleanArray) { }

private <!NOTHING_TO_INLINE!>inline<!> fun AtomicInt.f8(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicInt) {}

public class X private constructor(private val <!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicInt)
