// EXPECTED_REACHABLE_NODES: 495
package foo

fun box(): String {
    var c = 1

    do {
        if (c > 4) throw Exception("Timeout!")
        c++
    } while (fizz(c) % 2 == 0 || buzz(c) % 3 == 0)

    assertEquals("fizz(2);fizz(3);buzz(3);fizz(4);fizz(5);buzz(5);", pullLog())

    return "OK"
}