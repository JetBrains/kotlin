// MODULE: lib
// FILE: lib.kt

value class IntBox(val x: Int)
value class StringBox(val s: String)

// MODULE: main(lib)
// FILE: main.kt

import kotlin.native.internal.*
import kotlin.test.*

fun createIntBox(): Any = createUninitializedInstance<IntBox>()
fun createStringBox(): Any = createUninitializedInstance<StringBox>()

fun box(): String {
    val i = createIntBox()
    initInstance(i, IntBox(42))
    assertEquals(42, (i as IntBox).x)

    val s = createStringBox()
    initInstance(s, StringBox("OK"))
    return (s as StringBox).s
}
