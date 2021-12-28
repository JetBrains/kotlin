// WITH_STDLIB
// FILE: main.js
this.Math = withMocks(Math, {
    clz32(x) {
        var asUint = x >>> 0;
        if (asUint === 0) {
            return 32;
        }
        return 31 - (Math.log(asUint) / Math.LN2 | 0) | 0; // the "| 0" acts like math.floor
    }
})

// FILE: main.kt
fun box(): String {
    val result = 4.countLeadingZeroBits()

    assertEquals(result, 29)
    assertEquals(js("Math.clz32.called"), true)

    return "OK"
}
