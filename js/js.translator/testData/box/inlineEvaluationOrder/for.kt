// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1116
package foo

fun box(): String {
    for (i in fizz(1)..buzz(3)) {
        fizz(i)
    }

    assertEquals("fizz(1);buzz(3);fizz(1);fizz(2);fizz(3);", pullLog())

    return "OK"
}