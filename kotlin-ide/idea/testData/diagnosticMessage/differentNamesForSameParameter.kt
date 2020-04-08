// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES

interface A {
    fun foo(x: Int)
}

interface B {
    fun foo(y: Int) {}
}

class differentNamesForSameParameter : A, B
