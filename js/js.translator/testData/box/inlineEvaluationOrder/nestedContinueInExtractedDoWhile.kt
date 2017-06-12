// EXPECTED_REACHABLE_NODES: 494
package foo

private inline fun bar(predicate: (Int) -> Boolean) {
    var i = -1
    outer@do {
        i++
        if (i == 1) continue
        var j = -1
        do {
            ++j
            if (j == 1) {
                if (i == 3) continue@outer else continue
            }
            log("i$j")
        } while (j < 3)
        log("o$i")
    } while (predicate(i))
}

fun box(): String {
    bar {
        log("p$it")
        it < 5
    }
    assertEquals("i0;i2;i3;o0;p0;p1;i0;i2;i3;o2;p2;i0;p3;i0;i2;i3;o4;p4;i0;i2;i3;o5;p5;", pullLog())

    return "OK"
}