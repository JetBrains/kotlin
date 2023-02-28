// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// WITH_STDLIB
// FILE: functions.kt

package foo

@JsExport
external interface SomeExternalInterface

@JsExport
fun sum(x: Int, y: Int): Int =
    x + y

@JsExport
fun varargInt(vararg x: Int): Int =
    x.size

@JsExport
fun varargNullableInt(vararg x: Int?): Int =
    x.size

@JsExport
fun varargWithOtherParameters(x: String, vararg y: String, z: String): Int =
    x.length + y.size + z.length

@JsExport
fun varargWithComplexType(vararg x: (Array<IntArray>) -> Array<IntArray>): Int =
    x.size

@JsExport
fun sumNullable(x: Int?, y: Int?): Int =
    (x ?: 0) + (y ?: 0)

@JsExport
fun defaultParameters(a: String, x: Int = 10, y: String = "OK"): String =
    a + x.toString() + y

@JsExport
fun <T> generic1(x: T): T = x

@JsExport
fun <T> generic2(x: T?): Boolean = (x == null)

@JsExport
fun <T: String> genericWithConstraint(x: T): T = x

@JsExport
fun <T> genericWithMultipleConstraints(x: T): T
        where T : Comparable<T>,
              T : SomeExternalInterface,
              T : Throwable = x

@JsExport
fun <A, B, C, D, E> generic3(a: A, b: B, c: C, d: D): E? = null

@JsExport
inline fun inlineFun(x: Int, callback: (Int) -> Unit) {
    callback(x)
}

@JsExport
fun formatList(value: List<*>): String = value.joinToString(", ") { it.toString() }

@JsExport
fun createList(): List<*> = listOf(1, 2, 3)

// KT-53180
@JsExport
fun defaultParametersAtTheBegining(a: String = "Default Value", b: String) = "$a and $b"

@JsExport
fun nonDefaultParameterInBetween(a: String = "Default A", b: String, c: String = "Default C") = "$a and $b and $c"
