// EXPECTED_REACHABLE_NODES: 494
package foo

fun testLabelledBlock() {
    var c: Int = 0

    js("""
        block: {
            c = 1;
            break block;
            c = 2;
        }
    """)

    assertEquals(1, c, "testLabelledBlock")
}

fun testBreakInFor() {
    var c: Int = 0

    js("""
        outer: for (var i = 0; i < 10; i++) {
            for (var j = 0; j < 10; j++) {
                if (i === 1) {
                    break outer;
                }

                c += 1;
            }
        }
    """)

    assertEquals(10, c, "testBreakInFor")
}

fun testContinueInFor() {
    var c: Int = 0

    js("""
        outer: for (var i = 0; i < 10; i++) {
            for (var j = 0; j < 10; j++) {
                if (i >= 1) {
                    continue outer;
                }

                c += 1;
            }
        }
    """)

    assertEquals(10, c, "testContinueInFor")
}

fun box(): String {
    testLabelledBlock()
    testBreakInFor()
    testContinueInFor()

    return "OK"
}