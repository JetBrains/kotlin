// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1113
package foo

fun factorial(n: Int): Int = js("""
    var result = 1;
    var i = 1;

    while(i <= n) {
        result *= i++;
    }

    return result;
""")

fun box(): String {
    assertEquals(24, factorial(4))
    assertEquals(120, factorial(5))

    return "OK"
}