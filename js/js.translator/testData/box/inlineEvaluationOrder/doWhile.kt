// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1115
package foo

fun box(): String {
    var c = 2

    do {
        if (c > 4) throw Exception("Timeout!")
        c++
    } while (buzz(c) < 4)

    assertEquals("buzz(3);buzz(4);", pullLog())

    return "OK"
}