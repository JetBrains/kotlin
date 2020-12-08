// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1282
package foo

fun box(): String {
    var c: Int = 0

    js("""
        for (var i = 1; i < 6; i++) {
            if (i % 2 === 0) {
                continue;
            }

            c++;
        }
    """)

    assertEquals(3, c)

    return "OK"
}