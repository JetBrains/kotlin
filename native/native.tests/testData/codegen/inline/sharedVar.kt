// MODULE: lib
// FILE: lib.kt

package a

fun IntArray.forEachNoInline(block: (Int) -> Unit) = this.forEach { block(it) }

inline fun fold(initial: Int, values: IntArray, crossinline block: (Int, Int) -> Int): Int {
    var res = initial
    values.forEachNoInline {
        res = block(res, it)
    }
    return res
}

// MODULE: main(lib)
// FILE: main.kt

import a.*
import kotlin.test.*

fun box(): String {
    assertEquals(6, fold(0, intArrayOf(1, 2, 3)) { x, y -> x + y })
    return "OK"
}