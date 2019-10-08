// TARGET_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION

@file:JsExport

package foo

val _any: Any = Any()

val _unit: Unit = Unit

fun _nothing(): Nothing { throw Throwable() }

val _throwable: Throwable = Throwable()

val _string: String = "ZZZ"

val _boolean: Boolean = true

val _byte: Byte = 1.toByte()
val _short: Short = 1.toShort()
val _int: Int = 1
val _float: Float = 1.0f
val _double: Double = 1.0
// TODO: Char and Long

val _byte_array: ByteArray = byteArrayOf()
val _short_array: ShortArray = shortArrayOf()
val _int_array: IntArray = intArrayOf()
val _float_array: FloatArray = floatArrayOf()
val _double_array: DoubleArray = doubleArrayOf()

val _array_byte: Array<Byte> = emptyArray()
val _array_short: Array<Short> = emptyArray()
val _array_int: Array<Int> = emptyArray()
val _array_float: Array<Float> = emptyArray()
val _array_double: Array<Double> = emptyArray()
val _array_string: Array<String> = emptyArray()
val _array_boolean: Array<Boolean> = emptyArray()
val _array_array_string: Array<Array<String>> = arrayOf(emptyArray())
val _array_array_int_array: Array<Array<IntArray>> = arrayOf(arrayOf(intArrayOf()))

val _fun_unit: () -> Unit = { }
val _fun_int_unit: (Int) -> Unit = { x -> }

val _fun_boolean_int_string_intarray: (Boolean, Int, String) -> IntArray =
    { b, i, s -> intArrayOf(b.toString().length, i, s.length) }

val _curried_fun: (Int) -> (Int) -> (Int) -> (Int) -> (Int) -> Int =
    { x1 -> { x2 -> { x3 -> { x4 -> { x5 -> x1 + x2 + x3 + x4 + x5 } } } } }

val _higher_order_fun: ((Int) -> String, (String) -> Int) -> ((Int) -> Int) =
    { f1, f2 -> { x -> f2(f1(x)) } }


// Nullable types

val _n_any: Any? = Any()

// TODO:
// val _n_unit: Unit? = Unit

val _n_nothing: Nothing? = null

val _n_throwable: Throwable? = Throwable()

val _n_string: String? = "ZZZ"

val _n_boolean: Boolean? = true

val _n_byte: Byte? = 1.toByte()

// TODO: Char and Long

val _n_short_array: ShortArray? = shortArrayOf()

val _n_array_int: Array<Int>? = emptyArray()
val _array_n_int: Array<Int?> = emptyArray()
val _n_array_n_int: Array<Int?>? = emptyArray()

val _array_n_array_string: Array<Array<String>?> = arrayOf(arrayOf(":)"))

val _fun_n_int_unit: (Int?) -> Unit = { x -> }

val _fun_n_boolean_n_int_n_string_n_intarray: (Boolean?, Int?, String?) -> IntArray? =
    { b, i, s -> null }

val _n_curried_fun: (Int?) -> (Int?) -> (Int?) -> Int? =
    { x1 -> { x2 -> { x3 -> (x1 ?: 0) + (x2 ?: 0) + (x3 ?: 0) } } }

val _n_higher_order_fun: ((Int?) -> String?, (String?) -> Int?) -> ((Int?) -> Int?) =
    { f1, f2 -> { x -> f2(f1(x)) } }
