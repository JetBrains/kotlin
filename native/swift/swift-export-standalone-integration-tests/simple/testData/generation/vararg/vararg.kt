// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

fun simple(vararg s: String): String = s.joinToString(separator = "")

fun oneMore(vararg a: String, b: Int) {
    // callable as oneMore("x", "y", "z", b = 4)
}

fun withDefault(vararg a: String = arrayOf("X"), b: Int) {}

fun <T : Number> asNumberList(vararg x: T): List<T>? = null

fun Accessor.extension(vararg d: Double) {}

class Accessor(vararg val x: Int) {
    operator fun get(i: Int): Int = x[i]

    inner class Inner(val y: Double, vararg var z: Boolean)
}

fun setTag(vararg tag: String) {
    val a = "this is a test"
    a.length
}

fun setTag(tags: List<String>) {
    val a = "this is another test"
    a.length
}
