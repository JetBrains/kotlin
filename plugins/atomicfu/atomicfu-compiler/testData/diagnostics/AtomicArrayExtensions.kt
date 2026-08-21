// RENDER_DIAGNOSTICS_FULL_TEXT

// We're phasing out atomic array types from AFU
@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

// Extension properties are forbidden for AFU's atomic array types

import kotlinx.atomicfu.*
import kotlin.test.*

private val AtomicIntArray.<!ATOMIC_ARRAY_EXTENSION_PROPERTIES_ARE_FORBIDDEN!>firstElement<!>: Int
    get() = this[0].value

private var AtomicBooleanArray.<!ATOMIC_ARRAY_EXTENSION_PROPERTIES_ARE_FORBIDDEN!>firstElement<!>: Boolean
    get() = this[0].value
    set(value) {
        this[0].value = value
    }
