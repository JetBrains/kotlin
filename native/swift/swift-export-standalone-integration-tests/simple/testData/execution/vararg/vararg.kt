// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Vararg
// FILE: Vararg.kt

fun simple(a: String, vararg b: Char): String = buildString {
    append(a)
    b.forEach { append(it) }
}

fun produceCharArray() = charArrayOf('b', 'c', 'd')
