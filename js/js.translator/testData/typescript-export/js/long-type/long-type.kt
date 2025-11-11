// IGNORE_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +ContextParameters
// MODULE: JS_TESTS
// FILE: long-type.kt
package foo

@JsExport
val _long: Long = 1L

@JsExport
val _long_array: LongArray = longArrayOf()

@JsExport
val _array_long: Array<Long> = emptyArray()

@JsExport
<!MUST_BE_INITIALIZED!>var myVar: Long<!>
    get() = field
    set(value) { field = value + 1L }

// Nullable types
@JsExport
val _n_long: Long? = 1?.toLong()

// Functions with parameters
@JsExport
fun funWithLongParameters(a: Long, b: Long) = a + b

@JsExport
fun funWithLongDefaultParameters(a: Long = 1L, b: Long = a) = a + b

@JsExport
fun varargLong(vararg x: Long): Int =
    x.size

@JsExport
fun <T : Long> funWithTypeParameter(a: T, b: T) = a + b

@JsExport
fun <<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T<!>> funWithTypeParameterWithTwoUpperBounds(a: T, b: T) where T : Comparable<T>, T : Long = a + b

@JsExport
context(long: Long)
fun funWithContextParameter() = long

// Inline functions
@JsExport
inline fun inlineFun(a: Long, b: Long) = a + b

@JsExport
inline fun <T : Long> inlineFunWithTypeParameter(a: T, b: T) = a + b

@JsExport
inline fun inlineFunDefaultParameters(a: Long = 1L, b: Long = a) = a + b

// Function with extension receiver
@JsExport
fun Long.extensionFun() = this

// Local entities
@JsExport
fun globalFun(a: Long): Long {
    fun localFun(): Long = a

    return localFun()
}

@JsExport
object objectWithLong {
    val long = 1L
}

// Constructors
@JsExport
open class A(open val a: Long)

@JsExport
class B private constructor(val b: Long) {
    @JsName("snd_constructor")
    constructor() : this(1L) {}
}

@JsExport
class C(override val a: Long) : A(1L)

@JsExport
class D {
    class N(val n: Long)
    inner class I(val i: Long)
}

// Fun interface
@JsExport
fun interface funInterface {
    fun getLong(a: Long): Long
}

@JsExport
val funInterfaceInheritor1 = object : funInterface {
    override fun getLong(a: Long): Long = a
}

@JsExport
val funInterfaceInheritor2: funInterface = { it }