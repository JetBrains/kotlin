// FILE: main.js
Math.imul = undefined

// FILE: main.kt
fun box(): String {
    val a: Int = 2
    val b: Int = 42
    val c: Int = 44
    val d: Int = -2

    assertEquals(a * b, 84)
    assertEquals(a * c, 88)
    assertEquals(a * d, -4)

    return "OK"
}
