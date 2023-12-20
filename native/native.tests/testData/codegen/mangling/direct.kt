// MODULE: lib
// FILE: lib.kt

public fun mangle(l: List<Int>) = "Int direct $l"

public fun mangle(l: List<out Number>) = "out Number direct $l"

public fun mangle(l: List<*>) = "star direct $l"

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    val mutListInt = mutableListOf<Int>(1, 2, 3, 4)
    val mutListNum = mutableListOf<Number>(9, 10, 11, 12)
    val mutListAny = mutableListOf<Any>(5, 6, 7, 8)

    assertEquals("Int direct [1, 2, 3, 4]", mangle(mutListInt))
    assertEquals("out Number direct [9, 10, 11, 12]", mangle(mutListNum))
    assertEquals("star direct [5, 6, 7, 8]", mangle(mutListAny))

    return "OK"
}