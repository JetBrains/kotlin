// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// WITH_STDLIB
// LANGUAGE: +ContextParameters
// FILE: functions.kt

package foo

@JsExport
external interface SomeExternalInterface

@JsExport
fun sum(x: Int, y: Int): Int =
    x + y

@JsExport
fun varargByte(vararg x: Byte): Int =
    x.size

@JsExport
fun varargShort(vararg x: Short): Int =
    x.size

@JsExport
fun varargInt(vararg x: Int): Int =
    x.size

@JsExport
fun varargFloat(vararg x: Float): Int =
    x.size

@JsExport
fun varargDouble(vararg x: Double): Int =
    x.size

@JsExport
fun varargBoolean(vararg x: Boolean): Int =
    x.size

@JsExport
fun varargChar(vararg x: Char): Int =
    x.size

@JsExport
fun varargUByte(vararg x: UByte): Int =
    x.size

@JsExport
fun varargUShort(vararg x: UShort): Int =
    x.size

@JsExport
fun varargUInt(vararg x: UInt): Int =
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
fun formatList(value: MutableList<*>): String = value.joinToString(", ") { it.toString() }

@JsExport
fun createList(): MutableList<*> = mutableListOf(1, 2, 3)

// KT-53180
@JsExport
fun defaultParametersAtTheBegining(a: String = "Default Value", b: String) = "$a and $b"

@JsExport
fun nonDefaultParameterInBetween(a: String = "Default A", b: String, c: String = "Default C") = "$a and $b and $c"

// KQA-1835
@JsExport
class Scope1(val a: String) {
    fun getA(): String = a
}

@JsExport
class Scope2(val a: String) {
    fun getA(): String = a
}

@JsExport
context(scope1: Scope1, scope2: Scope2)
fun concatWithContextParameters() = scope1.getA() + scope2.getA()

@JsExport
context(scope1: Scope1)
fun Scope2.concatWithExtensionAndContextParameter() = scope1.getA() + getA()

@JsExport
fun Scope1.getWithExtension() = getA()

@JsExport
fun <A, B, R> context(a: A, b: B, block: context(A, B) () -> R): R = block(a, b)
