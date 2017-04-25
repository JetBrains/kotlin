// EXPECTED_REACHABLE_NODES: 491
package foo

// CHECK_LABELS_COUNT: function=box name=block count=2

fun box(): String {
    var i = 0
    var j = 0

    js("""
        block: {
            i++;
            break block;
            i++;
        }

        block: {
            j++;
            break block;
            j++;
        }
    """)

    assertEquals(1, i, "i")
    assertEquals(1, j, "j")
    return "OK"
}