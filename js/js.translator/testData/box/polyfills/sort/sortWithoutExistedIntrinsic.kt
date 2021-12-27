// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
var isLegacyBackend =
    typeof Kotlin != "undefined" && typeof Kotlin.kotlin != "undefined"

if (!isLegacyBackend) {
    Int32Array.prototype.sort = undefined
}
// FILE: main.kt
fun box(): String {
    val intArr = intArrayOf(5, 4, 3, 2, 1)
        .apply { sort { a, b -> a - b } }

    assertEquals(intArr.joinToString(","), "1,2,3,4,5")
    assertEquals(js("Int32Array.prototype.sort.called"), js("undefined"))

    return "OK"
}
