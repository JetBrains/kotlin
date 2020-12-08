// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1283
package foo

fun factorial(n: Int): Int = js("""
    var result = 1;

    for (var i = 1; i <= n; i++) {
        result *= i;
    }

    return result;
""")

fun box(): String {
    assertEquals(24, factorial(4))
    assertEquals(120, factorial(5))

    return "OK"
}