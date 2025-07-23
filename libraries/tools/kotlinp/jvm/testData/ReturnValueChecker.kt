// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: CHECKER
// IGNORE_BACKEND_K1: JVM_IR

// FILE: X.kt
@MustUseReturnValue
class X {
    fun foo(): Int = 42
    var bar: Int = 42

    fun unit() {}

    @IgnorableReturnValue fun ignorable(): String = ""
}

// FILE: topLevel.kt
@file:MustUseReturnValue

fun toplvl(): Int = 42
