// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283
package foo

fun countKeys(a: Array<Int>): Int = js("""
    var result = 0;

    for (var key in a) {
        result += 1;
    }

    return result;
""")

fun box(): String {
    assertEquals(3, countKeys(arrayOf(1,2,3)))
    assertEquals(4, countKeys(arrayOf(1,2,3,4)))

    return "OK"
}