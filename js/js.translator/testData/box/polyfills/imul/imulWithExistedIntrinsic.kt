// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.imul = function imul(a, b) {
    imul.called = true;
    return a * b
}

// FILE: main.kt
fun box(): String {
    val a: Int = 2
    val b: Int = 42
    val c: Int = a * b

    assertEquals(c, 84)
    assertEquals(js("Math.imul.called"), true)

    return "OK"
}
