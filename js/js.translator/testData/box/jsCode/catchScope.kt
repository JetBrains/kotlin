// EXPECTED_REACHABLE_NODES: 495
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