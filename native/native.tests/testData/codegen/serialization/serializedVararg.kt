// MODULE: lib
// FILE: lib.kt

val sb = StringBuilder()

fun bar(vararg x: Int) {
    x.forEach {
        sb.appendLine(it)
    }
    sb.appendLine("size: ${x.size}")
}

inline fun foo() = bar(17, 19, 23, *intArrayOf(29, 31))

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    foo()
    assertEquals("""
        17
        19
        23
        29
        31
        size: 5

    """.trimIndent(), sb.toString())
    return "OK"
}