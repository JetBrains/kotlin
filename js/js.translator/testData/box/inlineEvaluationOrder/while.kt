// EXPECTED_REACHABLE_NODES: 494
package foo

fun box(): String {
    var c = 2

    while (buzz(c) <= 4) {
        if (c > 4) throw Exception("Timeout!")
        c++
    }

    assertEquals("buzz(2);buzz(3);buzz(4);buzz(5);", pullLog())

    return "OK"
}