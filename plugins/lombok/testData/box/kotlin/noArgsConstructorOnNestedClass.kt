// FIR_DUMP
// DUMP_KT_IR
// FILE: Outer.kt

import lombok.NoArgsConstructor
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Outer {
    @NoArgsConstructor
    class Nested(var int: Int, var str: String)
}

class GenericOuter<T> {
    @NoArgsConstructor
    class Nested<S>(var param: S)
}

fun box(): String {
    val nested = Outer.Nested()
    assertEquals(0, nested.int)
    assertNull(nested.str)
    assertNull(GenericOuter.Nested<String>().param)

    return "OK"
}
