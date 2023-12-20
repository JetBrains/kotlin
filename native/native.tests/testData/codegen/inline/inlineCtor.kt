// MODULE: lib
// FILE: lib.kt

package a

fun foo(n: Int, block: (Int) -> Int): Int {
    val arr = IntArray(n) { block(it) }
    var sum = 0
    for (x in arr) sum += x
    return sum
}

// MODULE: main(lib)
// FILE: main.kt

import a.*
import kotlin.test.*

fun box(): String {
    assertEquals(42, foo(7) { it * 2 })
    return "OK"
}