// EXPECTED_REACHABLE_NODES: 487
fun box(): String {
    val i: Int? = 7
    val j: Int? = null
    val k = 7

    //verify errors
    if (i == 7) {
    }
    if (7 == i) {
    }

    if (j == 7) {
    }
    if (7 == j) {
    }

    if (i == k) {
    }
    if (k == i) {
    }

    if (j == k) {
    }
    if (k == j) {
    }
    return "OK"
}