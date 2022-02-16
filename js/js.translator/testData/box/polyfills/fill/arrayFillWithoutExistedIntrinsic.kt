// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Int32Array.prototype.fill = undefined;

// FILE: main.kt
fun box(): String {
    val int = IntArray(4).apply { fill(42) }

    assertEquals(int.joinToString(", "), "42, 42, 42, 42")
    assertEquals(js("Int32Array.prototype.fill.called"), js("undefined"))

    return "OK"
}
