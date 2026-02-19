// KIND: STANDALONE
// MODULE: Properties
// FILE: constants.kt
const val BOOLEAN_CONST: Boolean = true
const val CHAR_CONST: Char = 'A'
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

// FILE: lateinit.kt
class Foo(val value: Int) {
    override fun equals(other: Any?): Boolean {
        return other is Foo && other.value == value
    }
}

// Workaround absence of methods.
fun compare(a: Foo, b: Foo): Boolean {
    return a == b
}

lateinit var lateinitProperty: Foo
