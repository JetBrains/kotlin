// EXPECTED_REACHABLE_NODES: 1282
package foo

inline fun dangerCall() {
    assertEquals("A", "A")
    assertNotEquals("A", "A")
}

fun box(): String {
    if ("A" != "A") dangerCall()

    if ("A" == "A") {
        // Nothing to do
    } else dangerCall()

    while ("A" != "A") dangerCall()

    for (num in 0 until 0) dangerCall()

    when ("A") {
       "A" -> {}
        else -> dangerCall()
    }

    return "OK"
}

