// EXPECTED_REACHABLE_NODES: 492
package foo

fun factorial(n: Int): Int = js("""
    var result = 1;
    var i = 0;

    do {
        result *= ++i;
    } while(i < n);

    return result;
""")

fun box(): String {
    assertEquals(24, factorial(4))
    assertEquals(120, factorial(5))

    return "OK"
}