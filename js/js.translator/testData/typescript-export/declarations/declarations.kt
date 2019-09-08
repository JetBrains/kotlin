// TARGET_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION

@file:JsExport

package foo

// TODO: Test the same for member functions:

fun sum(x: Int, y: Int): Int =
    x + y

fun varargInt(vararg x: Int): Int =
    x.size

fun varargNullableInt(vararg x: Int?): Int =
    x.size

fun varargWithOtherParameters(x: String, vararg y: String, z: String): Int =
    x.length + y.size + z.length

fun varargWithComplexType(vararg x: (Array<IntArray>) -> Array<IntArray>): Int =
    x.size

fun sumNullable(x: Int?, y: Int?): Int =
    (x ?: 0) + (y ?: 0)

fun defaultParameters(x: Int = 10, y: String = "OK"): String =
    x.toString() + y

fun <T> generic1(x: T): T = x

fun <T> generic2(x: T?): Boolean = (x == null)

fun <A, B, C, D, E> generic3(a: A, b: B, c: C, d: D): E? = null

inline fun inlineFun(x: Int, callback: (Int) -> Unit) {
    callback(x)
}

// Properties

const val _const_val: Int = 1
val _val: Int = 1
var _var: Int = 1

val _valCustom: Int
    get() = 1

val _valCustomWithField: Int = 1
    get() = field + 1

var _varCustom: Int
    get() = 1
    set(value) {}

var _varCustomWithField: Int = 1
    get() = field * 10
    set(value) { field = value * 10 }

// Classes

class A
class A1(val x: Int)
class A2(val x: String, var y: Boolean)
class A3 {
    val x: Int = 100
}
class A4 {
    val _valCustom: Int
        get() = 1

    val _valCustomWithField: Int = 1
        get() = field + 1

    var _varCustom: Int
        get() = 1
        set(value) {}

    var _varCustomWithField: Int = 1
        get() = field * 10
        set(value) { field = value * 10 }
}