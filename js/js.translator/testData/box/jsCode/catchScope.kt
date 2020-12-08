// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6

// EXPECTED_REACHABLE_NODES: 1286
package foo

fun test(action: ()->Unit): String = js("""
    var e = { message: "ok" };

    try {
        action();
    } catch (e) {
        return e.message;
    }

    return e.message;
""")

fun box(): String {
    assertEquals("ok", test {})
    assertEquals("not ok", test { throw Exception("not ok") })

    return "OK"
}