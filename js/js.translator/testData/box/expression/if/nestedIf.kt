// EXPECTED_REACHABLE_NODES: 493
// JS: generate wrong code for nested if
// http://youtrack.jetbrains.com/issue/KT-5576

package foo

fun test2(a: Boolean, b: Boolean, c: Boolean) {
    val a =
            if (a) {
                if (b) {
                    "1"
                } else if (c) {
                    "2"
                } else {
                    throw Exception("Rest parameter must be array types")
                }
            }
            else {
                "3"
            }
}

fun box(): String {

    test2(true, true, false)
    var wasException = false
    try {
        test2(true, false, false)
    }
    catch(e: Exception) {
        wasException = true
    }
    assertEquals(true, wasException)

    return "OK"
}
