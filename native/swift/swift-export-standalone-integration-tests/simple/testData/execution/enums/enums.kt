// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Enum
// FILE: Enum.kt

enum class Enum(var i: Int, internal val s: String) {
    a(1, "str") {
        override fun print(): String = "$i - $s"
    },
    b(5, "rts") {
        override fun print(): String = "$s - $i"
    };

    abstract fun print(): String
}

enum class EmptyEnum
