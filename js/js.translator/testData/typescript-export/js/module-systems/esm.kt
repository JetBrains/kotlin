// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// JS_MODULE_KIND: ES
// WITH_STDLIB
// FILE: esm.kt

package foo

import kotlin.js.Promise

@JsExport
val value = 10

@JsExport
var variable = 10

@JsExport
class C(val x: Int) {
    fun doubleX() = x * 2
}

@JsExport
object O {
    val value = 10

    @JsStatic
    fun someStaticFunction() = "OK"

    @JsStatic
    var someStaticProperty = 42
}

@JsExport
object Parent {
    val value = 10
    class Nested(val value: Int) {
        @JsName("fromString")
        constructor(s: String): this(s.toInt())
    }

    enum class NestedEnum {
        A, B;
    }

    object NestedObject {
        val value = 10

        class Nested(val value: Int) {
            @JsName("fromString")
            constructor(s: String): this(s.toInt())
        }
    }
}

// KT-79926
@JsExport
interface AnInterfaceWithCompanion {
    companion object {
        private val privateValue = "OK"
        val someValue = privateValue
        const val constValue = "OK"
    }
}

// KT-82128
@JsExport
interface InterfaceWithNamedCompanion {
    companion object Name {
        private val privateValue = "OK"
        val someValue = privateValue
        const val constValue = "OK"

        @JsStatic
        val staticValue = "OK"
    }
}

// KT-84332
@JsExport
interface InterfaceWithNestedClass {
    fun createNested(value: Int): Nested

    fun consumeNested(nested: Nested): Int

    class Nested(val value: Int) {
        class DeepNested(val value: String)
    }

    class GenericNested<T>(val value: T)

    data class DataNested(val value: String)

    abstract class AbstractNested {
        abstract fun box(): String
    }

    class ConcreteNested : AbstractNested() {
        override fun box(): String = "OK"
    }

    class ConstructorWithDefaultsAndVarargs(val prefix: String = "", vararg val parts: String)

    value class NestedValue(val value: Int)
}

@JsExport
fun interface FunInterfaceWithNestedClass {
    fun run(value: String): String

    class Nested(val value: String)
}

@JsExport
fun createInterfaceNested(value: Int): InterfaceWithNestedClass.Nested =
    InterfaceWithNestedClass.Nested(value)

@JsExport
fun consumeInterfaceNested(nested: InterfaceWithNestedClass.Nested): Int =
    nested.value

@JsExport
fun createGenericInterfaceNested(value: String): InterfaceWithNestedClass.GenericNested<String> =
    InterfaceWithNestedClass.GenericNested(value)

@JsExport
fun copyInterfaceDataNested(nested: InterfaceWithNestedClass.DataNested): InterfaceWithNestedClass.DataNested =
    nested.copy(value = "copy")

@JsExport
fun createDeepInterfaceNested(value: String): InterfaceWithNestedClass.Nested.DeepNested =
    InterfaceWithNestedClass.Nested.DeepNested(value)

@JsExport
fun createConcreteInterfaceNested(): InterfaceWithNestedClass.ConcreteNested =
    InterfaceWithNestedClass.ConcreteNested()

@JsExport
fun createValueInterfaceNested(value: Int): InterfaceWithNestedClass.NestedValue =
    InterfaceWithNestedClass.NestedValue(value)

@JsExport
fun createFunInterfaceNested(value: String): FunInterfaceWithNestedClass.Nested =
    FunInterfaceWithNestedClass.Nested(value)

@JsExport
interface InterfaceWithCompanionWithStaticFun {
    companion object {
        @JsStatic
        fun bar() = "OK"
    }
}

@JsExport
interface I {
    fun foo(): String
}

@JsExport
interface InterfaceWithCompanionWithInheritor {
    companion object : I {
        override fun foo(): String = "OK"
    }
}

@JsExport
interface InterfaceWithCompanionWithInheritorAndStaticFun {
    companion object : I {
        @JsStatic
        override fun foo(): String = "OK"
    }
}

@JsExport
fun box(): String = "OK"

@JsExport
fun asyncList(): Promise<List<Int>> =
    Promise.resolve(listOf(1, 2))

@JsExport
fun arrayOfLists(): Array<List<Int>> =
    arrayOf(listOf(1, 2))

@JsExport.Default
fun justSomeDefaultExport() = "OK"
