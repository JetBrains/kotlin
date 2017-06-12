// EXPECTED_REACHABLE_NODES: 491
// CHECK_NOT_CALLED_IN_SCOPE: scope=box function=toString

package foo

fun box(): String {
    var right = 2
    var left = 3
    assertEquals("left = 3\nright = 2\nsum = 5\n", "left = $left\nright = $right\nsum = ${left + right}\n");
    return "OK"
}

