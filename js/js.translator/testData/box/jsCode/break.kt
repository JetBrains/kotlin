// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1282
package foo

fun box(): String {
    var c: Int = 0

    js("""
        for (var i = 0; i < 10; i++) {
            c = i;

            if (i === 3) {
                break;
            }
        }
    """)

    assertEquals(3, c)

    return "OK"
}