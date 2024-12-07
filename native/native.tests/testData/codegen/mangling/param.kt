// MODULE: lib
// FILE: lib.kt

public fun <T> mangle(l: T) where T: List<Int> = "Int param $l"

public fun <T> mangle(l: T) where T: List<out Number> = "out Number param $l"

public fun <T> mangle(l: T) where T: List<*> = "star param $l"

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    val mutListInt = mutableListOf<Int>(1, 2, 3, 4)
    val mutListNum = mutableListOf<Number>(9, 10, 11, 12)
    val mutListAny = mutableListOf<Any>(5, 6, 7, 8)

    assertEquals("Int param [1, 2, 3, 4]", mangle(mutListInt))
    assertEquals("out Number param [9, 10, 11, 12]", mangle(mutListNum))
    assertEquals("star param [5, 6, 7, 8]", mangle(mutListAny))

    return "OK"
}