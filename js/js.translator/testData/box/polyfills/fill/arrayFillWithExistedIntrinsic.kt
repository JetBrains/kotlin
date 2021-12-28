// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
if (!isLegacyBackend()) {
    Int32Array.prototype.fill = function fill(value) {
        fill.called = true;
        for (var i = 0; i < this.length; i++) {
            this[i] = value;
        }
        return this
    }
}

// FILE: main.kt
fun box(): String {
    val int = IntArray(4).apply { fill(42) }

    assertEquals(int.joinToString(", "), "42, 42, 42, 42")
    assertEquals(js("Int32Array.prototype.fill.called"), true)

    return "OK"
}
