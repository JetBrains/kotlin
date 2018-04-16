// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1114
package foo

inline fun block(p: () -> Int) = p()

fun createFunction(x: Int): () -> Int = { x }

fun box(): String {
    assertEquals(23, block(createFunction(23)))
    return "OK"
}