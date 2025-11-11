// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// WITH_STDLIB
// LANGUAGE: +ContextParameters
// FILE: functions.kt

@file:JsExport

package foo


external interface SomeExternalInterface


fun sum(x: Int, y: Int): Int =
    x + y


fun varargByte(vararg x: Byte): Int =
    x.size


fun varargShort(vararg x: Short): Int =
    x.size


fun varargInt(vararg x: Int): Int =
    x.size


fun varargFloat(vararg x: Float): Int =
    x.size


fun varargDouble(vararg x: Double): Int =
    x.size


fun varargBoolean(vararg x: Boolean): Int =
    x.size


fun varargChar(vararg x: Char): Int =
    x.size


fun varargUByte(vararg x: UByte): Int =
    x.size


fun varargUShort(vararg x: UShort): Int =
    x.size


fun varargUInt(vararg x: UInt): Int =
    x.size


fun varargNullableInt(vararg x: Int?): Int =
    x.size


fun varargWithOtherParameters(x: String, vararg y: String, z: String): Int =
    x.length + y.size + z.length


fun varargWithComplexType(vararg x: (Array<IntArray>) -> Array<IntArray>): Int =
    x.size


fun sumNullable(x: Int?, y: Int?): Int =
    (x ?: 0) + (y ?: 0)


fun defaultParameters(a: String, x: Int = 10, y: String = "OK"): String =
    a + x.toString() + y


fun <T> generic1(x: T): T = x


fun <T> generic2(x: T?): Boolean = (x == null)


fun <T: String> genericWithConstraint(x: T): T = x


fun <T> genericWithMultipleConstraints(x: T): T
        where T : Comparable<T>,
              T : SomeExternalInterface,
              T : Throwable = x


fun <A, B, C, D, E> generic3(a: A, b: B, c: C, d: D): E? = null


inline fun inlineFun(x: Int, callback: (Int) -> Unit) {
    callback(x)
}


fun formatList(value: MutableList<*>): String = value.joinToString(", ") { it.toString() }


fun createList(): MutableList<*> = mutableListOf(1, 2, 3)

// KT-53180

fun defaultParametersAtTheBegining(a: String = "Default Value", b: String) = "$a and $b"


fun nonDefaultParameterInBetween(a: String = "Default A", b: String, c: String = "Default C") = "$a and $b and $c"

// KQA-1835

class Scope1(val a: String) {
    fun getA(): String = a
}


class Scope2(val a: String) {
    fun getA(): String = a
}


context(scope1: Scope1, scope2: Scope2)
fun concatWithContextParameters() = scope1.getA() + scope2.getA()


context(scope1: Scope1)
fun Scope2.concatWithExtensionAndContextParameter() = scope1.getA() + getA()


fun Scope1.getWithExtension() = getA()


fun <A, B, C, D, R> allParameters(a: A, b: B, c: C, d: D, block: context(A, B) C.(d: D) -> R): R = block(a, b, c, d)