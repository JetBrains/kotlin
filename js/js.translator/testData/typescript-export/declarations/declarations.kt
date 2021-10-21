// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: declarations.kt

package foo

// TODO: Test the same for member functions:

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
fun defaultParameters(x: Int = 10, y: String = "OK"): String =
    x.toString() + y

@JsExport
fun <T> generic1(x: T): T = x

@JsExport
fun <T> generic2(x: T?): Boolean = (x == null)

@JsExport
fun <A, B, C, D, E> generic3(a: A, b: B, c: C, d: D): E? = null

@JsExport
inline fun inlineFun(x: Int, callback: (Int) -> Unit) {
    callback(x)
}

// Properties

@JsExport
const val _const_val: Int = 1

@JsExport
val _val: Int = 1

@JsExport
var _var: Int = 1

@JsExport
val _valCustom: Int
    get() = 1

@JsExport
val _valCustomWithField: Int = 1
    get() = field + 1

@JsExport
var _varCustom: Int
    get() = 1
    set(value) {}

@JsExport
var _varCustomWithField: Int = 1
    get() = field * 10
    set(value) { field = value * 10 }

// Classes

@JsExport
class A

@JsExport
class A1(val x: Int)

@JsExport
class A2(val x: String, var y: Boolean)

@JsExport
class A3 {
    val x: Int = 100
}

@JsExport
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


@JsExport
object O0

@JsExport
object O {
    val x = 10
    @JsName("foo")  // TODO: Should work without JsName
    fun foo() = 20
}

@JsExport
fun takesO(o: O): Int =
    O.x + O.foo()

@JsExport
class KT_37829 {
    companion object {
        val x = 10
    }
}

// See KT-47376, KT-39364
@JsExport
sealed class TestSealed(val name: String) {
    class AA : TestSealed("AA") {
        fun bar(): String = "bar"
    }
    class BB : TestSealed("BB") {
        fun baz(): String = "baz"
    }
}

// See KT-39364
@JsExport
abstract class TestAbstract(val name: String) {
    class AA : TestAbstract("AA") {
        fun bar(): String = "bar"
    }
    class BB : TestAbstract("BB") {
        fun baz(): String = "baz"
    }
}

@JsExport
data class TestDataClass(val name: String) {
    class Nested {
        val prop: String = "hello"
    }
}

@JsExport
enum class TestEnumClass(val constructorParameter: String) {
    A("aConstructorParameter"),
    B("bConstructorParameter");

    val foo = ordinal

    fun bar(value: String) = value

    fun bay() = name

    class Nested {
        val prop: String = "hello2"
    }
}