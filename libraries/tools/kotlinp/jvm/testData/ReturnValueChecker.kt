// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: CHECKER

// FILE: X.kt
@MustUseReturnValues
class X {
    fun foo(): Int = 42
    var bar: Int = 42

    fun unit() {}

    @IgnorableReturnValue fun ignorable(): String = ""
}

fun unspecified(): Int = 42

// FILE: topLevel.kt
@file:MustUseReturnValues

fun toplvl(): Int = 42
