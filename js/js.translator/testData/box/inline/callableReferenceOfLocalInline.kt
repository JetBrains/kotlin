// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1114
package foo

fun multiplyBy(x: Int): () -> ((Int) -> Int) {
    inline fun applyMultiplication(y: Int): Int = x * y

    return { ::applyMultiplication }
}

fun box(): String {
    assertEquals(6, multiplyBy(2)()(3))

    return "OK"
}