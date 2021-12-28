// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
if (!isLegacyBackend()) {
    Math.clz32 = function clz32(x) {
        clz32.called = true;
        var asUint = x >>> 0;
        if (asUint === 0) {
            return 32;
        }
        return 31 - (Math.log(asUint) / Math.LN2 | 0) | 0; // the "| 0" acts like math.floor
    }
}

// FILE: main.kt
fun box(): String {
    val result = 4.countLeadingZeroBits()

    assertEquals(result, 29)
    assertEquals(js("Math.clz32.called"), true)

    return "OK"
}
