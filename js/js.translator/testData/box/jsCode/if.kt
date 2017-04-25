// EXPECTED_REACHABLE_NODES: 492
package foo

fun testIf(flag: Boolean): Int = js("""
    if (flag)
        return 1;
    else
        return -1;

    return 0;
""")


fun box(): String {
    assertEquals(1, testIf(true))
    assertEquals(-1, testIf(false))

    return "OK"
}