// MODULE: lib
// FILE: lib.kt

inline val Int.prop get() = SomeDataClass(second = this)

data class SomeDataClass(val first: Int = 17, val second: Int = 19, val third: Int = 23)

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    assertEquals(SomeDataClass(first = 17, second = 666, third = 23), 666.prop)
    return "OK"
}