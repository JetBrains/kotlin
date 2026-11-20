// JDK_KIND: FULL_JDK_21
// MIN_JAVA_VERSION: 16
// CORRECT_ERROR_TYPES

@file:Suppress("UNRESOLVED_REFERENCE")
package test

@JvmRecord
data class R(val x: Unresolved, val y: List<Unresolved2>) {
    constructor() : this(null!!, emptyList())
}
