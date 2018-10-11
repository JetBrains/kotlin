// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1294
package foo

enum class A {
    B(2);

    val c: Int

    constructor (a: Int, b: Int = 3) {
        c = a + b
    }
}

fun box(): String {
    assertEquals(5, A.B.c)
    return "OK"
}