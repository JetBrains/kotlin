// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {
    val c = 3
    val d = 5
    var z = 0
    when(c) {
        5, 3 -> z++;
        else -> {
            z = -1000;
        }
    }

    when(d) {
        5, 3 -> z++;
        else -> {
            z = -1000;
        }
    }
    assertEquals(2, z)
    return "OK"
}
