// EXPECTED_REACHABLE_NODES: 894
package foo


fun testDownTo() {
    var elems = mutableListOf<Int>()
    for (i in 4 downTo 0) {
        elems.add(i)
    }
    assertTrue(elems[0] == 4 && elems[1] == 3 && elems[2] == 2 && elems[3] == 1 &&  elems[4] == 0)
}

fun testDownToDoesNotIterate() {
    for (i in 0 downTo 1) {
        fail()
    }
}

fun box(): String {
    testDownTo()
    testDownToDoesNotIterate()

    return "OK"
}
