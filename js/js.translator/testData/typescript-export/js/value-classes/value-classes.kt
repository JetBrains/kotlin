// TSC_TARGET: es2020
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// WITH_STDLIB
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowExportingValueClasses
// MODULE: JS_TESTS
// FILE: value-classes.kt

package foo

// Basic value class with Int
@JsExport
value class IntValueClass(val value: Int)

// Value class with String
@JsExport
value class StringValueClass(val name: String)

// Value class with Double
@JsExport
value class DoubleValueClass(val amount: Double)

// Value class with Boolean
@JsExport
value class BooleanValueClass(val flag: Boolean)

// Value class with nullable type
@JsExport
value class NullableValueClass(val data: String?)

// Generic value class
@JsExport
value class GenericValueClass<T>(val item: T)

// Value class with custom methods
@JsExport
value class ValueClassWithMethods(val number: Int) {
    fun double(): Int = number * 2
    fun add(other: Int): Int = number + other
}

// Value class with companion object
@JsExport
value class ValueClassWithCompanion(val value: String) {
    companion object {
        const val DEFAULT = "default"
        fun create(s: String): ValueClassWithCompanion = ValueClassWithCompanion(s)
    }
}

// Value class as function parameter
@JsExport
fun acceptValueClass(v: IntValueClass): Int = v.value

// Value class as return type
@JsExport
fun createValueClass(x: Int): IntValueClass = IntValueClass(x)

// Multiple value class parameters
@JsExport
fun combineValueClasses(a: IntValueClass, b: IntValueClass): Int = a.value + b.value

// Value class in generic function
@JsExport
fun <T> useGenericValueClass(g: GenericValueClass<T>): T = g.item

// Value class with another value class as property
@JsExport
value class NestedValueClass(val inner: IntValueClass)

// Regular class with value class property
@JsExport
class ClassWithValueProperty(val data: StringValueClass)

// Regular class with value class methods
@JsExport
class ClassWithValueMethods {
    fun produceValue(): IntValueClass = IntValueClass(42)
    fun consumeValue(v: IntValueClass): Int = v.value
}

// Array of value classes
@JsExport
fun createValueArray(): Array<IntValueClass> = arrayOf(
    IntValueClass(1),
    IntValueClass(2),
    IntValueClass(3)
)

// Nullable value class parameter
@JsExport
fun acceptNullableValueClass(v: IntValueClass?): Int? = v?.value

// Value class equality
@JsExport
fun compareValueClasses(a: IntValueClass, b: IntValueClass): Boolean = a == b

// Value class implementing interface
@JsExport
interface HasValue {
    val value: Int
}

@JsExport
value class ValueClassWithInterface(override val value: Int) : HasValue

// Value class with secondary constructors
@JsExport
value class ValueClassWithConstructors(val data: String) {
    @JsName("createFromNumber")
    constructor(number: Int) : this(number.toString())
}

// Collections of value classes
@JsExport
fun createValueClassList(): List<IntValueClass> = listOf(
    IntValueClass(1),
    IntValueClass(2),
    IntValueClass(3)
)

@JsExport
fun createValueClassSet(): Set<StringValueClass> = setOf(
    StringValueClass("a"),
    StringValueClass("b"),
    StringValueClass("c")
)

@JsExport
fun createValueClassMap(): Map<IntValueClass, StringValueClass> = mapOf(
    IntValueClass(1) to StringValueClass("one"),
    IntValueClass(2) to StringValueClass("two")
)

@JsExport
fun acceptValueClassList(list: List<IntValueClass>): Int = list.sumOf { it.value }

@JsExport
fun acceptValueClassArray(arr: Array<IntValueClass>): Int = arr.sumOf { it.value }

// Value class in collections with other types
@JsExport
fun mixedCollection(): List<Any> = listOf(
    IntValueClass(42),
    "string",
    100
)

// Class with collection properties of value classes
@JsExport
class ClassWithValueCollections {
    val list: List<IntValueClass> = listOf(IntValueClass(1), IntValueClass(2))
    val array: Array<StringValueClass> = arrayOf(StringValueClass("a"), StringValueClass("b"))
    var mutableList: MutableList<IntValueClass> = mutableListOf(IntValueClass(10))
    
    fun addToList(v: IntValueClass) {
        mutableList.add(v)
    }
    
    fun getListSize(): Int = mutableList.size
}

// Nested collections with value classes
@JsExport
fun nestedValueClassCollection(): List<List<IntValueClass>> = listOf(
    listOf(IntValueClass(1), IntValueClass(2)),
    listOf(IntValueClass(3), IntValueClass(4))
)

// Value class wrapping a collection
@JsExport
value class ValueClassWithCollection(val items: List<Int>)

@JsExport
fun createValueClassWithCollection(): ValueClassWithCollection = 
    ValueClassWithCollection(listOf(1, 2, 3))

// Value class as map key and value
@JsExport
fun useValueClassAsMapKey(map: Map<IntValueClass, String>): String? = 
    map[IntValueClass(1)]

@JsExport
fun useValueClassAsMapValue(map: Map<String, IntValueClass>): Int? = 
    map["key"]?.value

// Pair and Triple with value classes
@JsExport
fun createPairWithValueClass(): Pair<IntValueClass, StringValueClass> = 
    IntValueClass(42) to StringValueClass("answer")

@JsExport
fun createTripleWithValueClass(): Triple<IntValueClass, StringValueClass, BooleanValueClass> = 
    Triple(IntValueClass(1), StringValueClass("test"), BooleanValueClass(true))

@JsExport
fun acceptPairWithValueClass(pair: Pair<IntValueClass, IntValueClass>): Int = 
    pair.first.value + pair.second.value
