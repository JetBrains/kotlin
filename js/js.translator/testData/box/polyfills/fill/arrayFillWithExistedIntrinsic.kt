// WITH_STDLIB
// FILE: main.js
this.Int32Array = withMockedPrototype(Int32Array, {
    fill(value) {
        for (var i = 0; i < this.length; i++) {
            this[i] = value;
        }
        return this
    }
})

// FILE: main.kt
fun box(): String {
    val int = IntArray(4).apply { fill(42) }

    assertEquals(int.joinToString(", "), "42, 42, 42, 42")
    assertEquals(js("Int32Array.prototype.fill.called"), true)

    return "OK"
}
