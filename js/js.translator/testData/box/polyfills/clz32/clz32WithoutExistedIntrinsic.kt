// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.clz32 = undefined;

// FILE: main.kt
fun box(): String {
    val result = 4.countLeadingZeroBits()

    assertEquals(result, 29)
    assertEquals(js("Math.clz32.called"), js("undefined"))

    return "OK"
}
