// MODULE: lib
// FILE: lib.kt

package a

enum class A(val x: Int) {
    Z1(42),
    Z2(117),
    Z3(-1)
}

// MODULE: main(lib)
// FILE: main.kt

import a.*
import kotlin.test.*

fun box(): String {
    assertEquals(42, A.Z1.x)
    assertEquals(117, A.valueOf("Z2").x)
    assertEquals(-1, A.values()[2].x)
    return "OK"
}