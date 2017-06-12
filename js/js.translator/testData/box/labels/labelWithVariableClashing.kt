// EXPECTED_REACHABLE_NODES: 493
package foo

// CHECK_LABELS_COUNT: function=test1 name=loop count=1
// CHECK_LABELS_COUNT: function=test2 name=loop count=1

fun test1() {
    var `loop$` = 0

    loop@ for (i in 1..10) {
        `loop$` = i
        if (i == 5) break@loop
    }

    assertEquals(5, `loop$`, "test1")
}

fun test2() {
    var loop = 0

    loop@ for (i in 1..10) {
        loop = i
        if (i == 5) break@loop
    }

    assertEquals(5, loop, "test2")
}


fun box(): String {
    test1()
    test2()

    return "OK"
}