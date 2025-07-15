// ISSUE: KT-6675
// IGNORE_BACKEND: JS_IR
// ^ Only works in ES6s

// FILE: main.kt
external interface LongBox {
    var value: Long
}

external fun LongBox(value: Long): LongBox

external fun add(a: Long, b: Long): Long

fun box(): String {
    val longBox = LongBox(0L)
    assertEquals(0L, longBox.value)
    longBox.value = Long.MAX_VALUE
    assertEquals(Long.MAX_VALUE, longBox.value)
    assertEquals(9223372036854775807.0, longBox.value.toDouble())
    longBox.value -= 1L
    assertEquals(9223372036854775806.0, longBox.value.toDouble())

    assertEquals(46L, add(12L, 34L))
    assertEquals(43.0, add(42L, 1L).toDouble())
    assertEquals(-2147483648, add(Int.MAX_VALUE.toLong(), 1L).toInt())
    assertTrue(add(42L, 1L) is Long)

    return "OK"
}

// FILE: lib.js

function LongBox(value) {
    return {value:value}
}

function add(a, b) {
    return a + b;
}
