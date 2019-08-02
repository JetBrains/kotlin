// TARGET_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS

@file:JsExport

package foo

fun box(): String =
    "OK"

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

fun <A, B, C, D, E> generic3(a: A, b: B, c: C, d: C): E? = null

inline fun inlineFun(x: Int, callback: (Int) -> Unit) {
    callback(x)
}

// Properties

const val _const_val: Int = 1
val _val: Int = 1
var _var: Int = 1


// Classes

class A
class A1(val x: Int)
class A2(val x: String, var y: Boolean)
class A3 {
    val x: Int = 100
}