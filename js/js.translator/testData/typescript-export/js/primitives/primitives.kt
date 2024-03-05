// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: primitives.kt

package foo

@JsExport
val _any: Any = Any()

@JsExport
fun _nothing(): Nothing { throw Throwable() }

@JsExport
val _throwable: Throwable = Throwable()

@JsExport
val _string: String = "ZZZ"

@JsExport
val _boolean: Boolean = true

@JsExport
val _byte: Byte = 1.toByte()
@JsExport
val _short: Short = 1.toShort()
@JsExport
val _int: Int = 1
@JsExport
val _float: Float = 1.0f
@JsExport
val _double: Double = 1.0
// TODO: Char and Long

@JsExport
val _byte_array: ByteArray = byteArrayOf()
@JsExport
val _short_array: ShortArray = shortArrayOf()
@JsExport
val _int_array: IntArray = intArrayOf()
@JsExport
val _float_array: FloatArray = floatArrayOf()
@JsExport
val _double_array: DoubleArray = doubleArrayOf()

@JsExport
val _array_byte: Array<Byte> = emptyArray()
@JsExport
val _array_short: Array<Short> = emptyArray()
@JsExport
val _array_int: Array<Int> = emptyArray()
@JsExport
val _array_float: Array<Float> = emptyArray()
@JsExport
val _array_double: Array<Double> = emptyArray()
@JsExport
val _array_string: Array<String> = emptyArray()
@JsExport
val _array_boolean: Array<Boolean> = emptyArray()
@JsExport
val _array_array_string: Array<Array<String>> = arrayOf(emptyArray())
@JsExport
val _array_array_int_array: Array<Array<IntArray>> = arrayOf(arrayOf(intArrayOf()))

@JsExport
val _fun_unit: () -> Unit = { }
@JsExport
val _fun_int_unit: (Int) -> Unit = { x -> }

@JsExport
val _fun_boolean_int_string_intarray: (Boolean, Int, String) -> IntArray =
    { b, i, s -> intArrayOf(b.toString().length, i, s.length) }

@JsExport
val _curried_fun: (Int) -> (Int) -> (Int) -> (Int) -> (Int) -> Int =
    { x1 -> { x2 -> { x3 -> { x4 -> { x5 -> x1 + x2 + x3 + x4 + x5 } } } } }

@JsExport
val _higher_order_fun: ((Int) -> String, (String) -> Int) -> ((Int) -> Int) =
    { f1, f2 -> { x -> f2(f1(x)) } }


// Nullable types

@JsExport
val _n_any: Any? = Any()

// TODO:
// val _n_unit: Unit? = Unit

@JsExport
val _n_nothing: Nothing? = null

@JsExport
val _n_throwable: Throwable? = Throwable()

@JsExport
val _n_string: String? = "ZZZ"

@JsExport
val _n_boolean: Boolean? = true

@JsExport
val _n_byte: Byte? = 1.toByte()

// TODO: Char and Long

@JsExport
val _n_short_array: ShortArray? = shortArrayOf()

@JsExport
val _n_array_int: Array<Int>? = emptyArray()
@JsExport
val _array_n_int: Array<Int?> = emptyArray()
@JsExport
val _n_array_n_int: Array<Int?>? = emptyArray()

@JsExport
val _array_n_array_string: Array<Array<String>?> = arrayOf(arrayOf(":)"))

@JsExport
val _fun_n_int_unit: (Int?) -> Unit = { x -> }

@JsExport
val _fun_n_boolean_n_int_n_string_n_intarray: (Boolean?, Int?, String?) -> IntArray? =
    { b, i, s -> null }

@JsExport
val _n_curried_fun: (Int?) -> (Int?) -> (Int?) -> Int? =
    { x1 -> { x2 -> { x3 -> (x1 ?: 0) + (x2 ?: 0) + (x3 ?: 0) } } }


@JsExport
val _n_higher_order_fun: ((Int?) -> String?, (String?) -> Int?) -> ((Int?) -> Int?) =
    { f1, f2 -> { x -> f2(f1(x)) } }
