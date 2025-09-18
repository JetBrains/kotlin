// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

fun simple(vararg s: String): String = s.joinToString(separator = "")

class Accessor(vararg val x: Int) {
    operator fun get(i: Int): Int = x[i]
}
