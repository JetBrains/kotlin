// CORRECT_ERROR_TYPES

@file:Suppress("UNRESOLVED_REFERENCE")
package test

typealias TA<W> = U1<W>

data class C(val x: U2, val y: List<U3>, val z: TA<U4>)
