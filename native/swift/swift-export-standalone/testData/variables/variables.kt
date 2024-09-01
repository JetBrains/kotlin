// KIND: STANDALONE
// MODULE: main
// FILE: main.kt
package namespace.main

val foo: Int get() = 10

var bar: Int = 20

fun foobar(param: Int): Int = foo + bar + param

// FILE: lateinit.kt

private class Foo

var foo: Any = Foo()
lateinit var lateinit_foo: Any

// FILE: constants.kt
const val BOOLEAN_CONST: Boolean = true
const val BYTE_CONST: Byte = 1
const val SHORT_CONST: Short = 2
const val INT_CONST: Int = 3
const val LONG_CONST: Long = 4L
const val FLOAT_CONST: Float = 5.0f
const val DOUBLE_CONST: Double = 6.0

const val UBYTE_CONST: UByte = 1u
const val USHORT_CONST: UShort = 2u
const val UINT_CONST: UInt = 3u
const val ULONG_CONST: ULong = 4uL

const val CHAR_CONST: Char = 'A'
// FILE: no_package.kt
val baz: Int = 30

// FILE: unsupported.kt
const val STRING_CONST: String = "Hello, World!"
