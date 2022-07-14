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
              T : TestInterface,
              T : Throwable = x

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
class A5<T>(val value: T) {
    fun test(): T = value
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


@JsExport
interface TestInterface {
    val value: String
    fun getOwnerName(): String
}

@JsExport
interface AnotherExportedInterface

@JsExport
open class TestInterfaceImpl(override val value: String) : TestInterface {
    override fun getOwnerName() = "TestInterfaceImpl"
}

@JsExport
class ChildTestInterfaceImpl(): TestInterfaceImpl("Test"), AnotherExportedInterface

@JsExport
fun processInterface(test: TestInterface): String {
    return "Owner ${test.getOwnerName()} has value '${test.value}'"
}

@JsExport
class OuterClass {
    enum class NestedEnum {
        A,
        B
    }
}

@JsExport
open class KT38262 {
    fun then(): Int = 42
    fun catch(): Int = 24
}

@JsExport
@JsName("JsNameTest")
class __JsNameTest private constructor() {
    @JsName("value")
    val __value = 4

    @JsName("runTest")
    fun __runTest(): String {
        return "JsNameTest"
    }

    companion object {
        @JsName("create")
        fun __create(): __JsNameTest {
           return __JsNameTest()
        }

        @JsName("createChild")
        fun __createChild(value: Int): __NestJsNameTest {
           return  __NestJsNameTest(value)
        }
    }

    @JsName("NestedJsName")
    class __NestJsNameTest(@JsName("value") val __value: Int)
}

@JsExport
data class KT39423(
    val a: String,
    val b: Int? = null
)

@JsExport
object Parent {
    object Nested1 {
        val value: String = "Nested1"
        class Nested2 {
            companion object {
                class Nested3
            }
        }
    }
}

@JsExport
fun getParent(): Parent {
    return Parent
}

@JsExport
fun createNested1(): Parent.Nested1 {
    return Parent.Nested1
}

@JsExport
fun createNested2(): Parent.Nested1.Nested2 {
    return Parent.Nested1.Nested2()
}

@JsExport
fun createNested3(): Parent.Nested1.Nested2.Companion.Nested3 {
    return Parent.Nested1.Nested2.Companion.Nested3()
}

@JsExport
class GenericClassWithConstraint<T: TestInterface>(val test: T)