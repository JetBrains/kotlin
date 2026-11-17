// JDK_KIND: FULL_JDK_21
// MIN_JAVA_VERSION: 16
// CORRECT_ERROR_TYPES

@file:Suppress("UNRESOLVED_REFERENCE")
package test

@JvmRecord
data class R(val x: Unresolved, val y: List<Unresolved2>) {
    constructor() : this(null!!, emptyList())
}

// A vararg component's written type is its bare element; the stub must still render it as vararg.
@JvmRecord
data class VarargRecord(vararg val xs: Unresolved)

@JvmRecord
data class VarargGenericRecord(vararg val ys: List<Unresolved2>)

@JvmRecord
data class VarargArrayRecord(vararg val xa: Array<Unresolved>)
