// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1283
package foo

fun testSwitch(number: Int): String = js("""
    var result;

    switch(number) {
        case 1:
            result = "one";
            break;
        case 2:
            result = "two";
            break;
        default:
            result = "don't know";
            break;
    }

    return result;
""")

fun box(): String {
    assertEquals("one", testSwitch(1))
    assertEquals("two", testSwitch(2))
    assertEquals("don't know", testSwitch(3))

    return "OK"
}