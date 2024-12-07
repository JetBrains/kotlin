// CORRECT_ERROR_TYPES

@file:Suppress("UNRESOLVED_REFERENCE")

package test

abstract class A(val s: String)

class B : A(C.foo())
