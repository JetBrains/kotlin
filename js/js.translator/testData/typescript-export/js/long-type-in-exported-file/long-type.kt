// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// IGNORE_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +ContextParameters
// MODULE: JS_TESTS
// FILE: long-type.kt

@file:JsExport
package foo


val _long: Long = 1L


val _long_array: LongArray = longArrayOf()


val _array_long: Array<Long> = emptyArray()


<!MUST_BE_INITIALIZED!>var myVar: Long<!>
    get() = field
    set(value) { field = value + 1L }

// Nullable types

val _n_long: Long? = 1?.toLong()

// Functions with parameters

fun funWithLongParameters(a: Long, b: Long) = a + b


fun funWithLongDefaultParameters(a: Long = 1L, b: Long = a) = a + b


fun varargLong(vararg x: Long): Int =
    x.size


fun <T : Long> funWithTypeParameter(a: T, b: T) = a + b


fun <<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T<!>> funWithTypeParameterWithTwoUpperBounds(a: T, b: T) where T : Comparable<T>, T : Long = a + b


context(long: Long)
fun funWithContextParameter() = long

// Inline functions

inline fun inlineFun(a: Long, b: Long) = a + b


inline fun <T : Long> inlineFunWithTypeParameter(a: T, b: T) = a + b


inline fun inlineFunDefaultParameters(a: Long = 1L, b: Long = a) = a + b

// Function with extension receiver

fun Long.extensionFun() = this

// Local entities

fun globalFun(a: Long): Long {
    fun localFun(): Long = a

    return localFun()
}


object objectWithLong {
    val long = 1L
}

// Constructors

open class A(open val a: Long)


class B private constructor(val b: Long) {
    @JsName("snd_constructor")
    constructor() : this(1L) {}
}


class C(override val a: Long) : A(1L)


class D {
    class N(val n: Long)
    inner class I(val i: Long)
}

// Fun interface

fun interface funInterface {
    fun getLong(a: Long): Long
}


val funInterfaceInheritor1 = object : funInterface {
    override fun getLong(a: Long): Long = a
}


val funInterfaceInheritor2: funInterface = { it }