// MODULE: lib
// FILE: lib.kt

value class IntWrapper(val x: Int) {
    constructor(s: String): this(s.length)
}

value class StringWrapper(val s: String)

// MODULE: main(lib)
// FILE: main.kt

import kotlin.native.internal.*
import kotlin.test.*

fun createIntWrapper(): Any = IntWrapper(117)
fun createStringWrapper(): Any = StringWrapper("fail")

fun foo(x: Int?) {
    x?.let {
        val i = createIntWrapper()
        initInstance(i, IntWrapper(it))
        assertEquals((i as IntWrapper).x, 42)
    }
}

fun box(): String {
    val i = createIntWrapper()
    initInstance(i, IntWrapper(42))
    assertEquals((i as IntWrapper).x, 42)
    initInstance(i, IntWrapper("zzz"))
    assertEquals((i as IntWrapper).x, 3)

    foo(42)

    val s = createStringWrapper()
    initInstance(s, StringWrapper("OK"))
    return (s as StringWrapper).s
}