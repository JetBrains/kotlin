// IGNORE_BACKEND: JS
// FILE: main.js
this.Math = withMocks(Math, {
   imul(a, b) {
        return a * b
    }
})

// FILE: main.kt
fun box(): String {
    val a: Int = 2
    val b: Int = 42
    val c: Int = a * b

    assertEquals(c, 84)
    assertEquals(js("Math.imul.called"), true)

    return "OK"
}
