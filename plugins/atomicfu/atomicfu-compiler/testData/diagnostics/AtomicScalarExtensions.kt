// RENDER_DIAGNOSTICS_FULL_TEXT

@file:Suppress("NOTHING_TO_INLINE")

import kotlinx.atomicfu.*
import kotlin.test.*

// Extension properties should be inline and non public
private val AtomicInt.<!ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE!>p1<!>: Int get() = value * 2
private inline val AtomicInt.p2: Int get() = value * 2
public inline val AtomicInt.<!ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE!>p3<!>: Int get() = value * 2
internal inline val AtomicInt.p4: Int get() = value * 2
@PublishedApi internal inline val AtomicLong.<!ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE!>p5<!>: Long get() = value * 2
