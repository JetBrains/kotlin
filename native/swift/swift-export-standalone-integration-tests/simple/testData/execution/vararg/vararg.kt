// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Vararg
// FILE: Vararg.kt

fun simple(a: String, vararg b: Int): String = buildString {
    append(a)
    b.forEach { append(it) }
}

class Accessor(vararg val x: Float) {
    operator fun get(i: Int): Float = x[i]
}
