// EXPECTED_REACHABLE_NODES: 491
// CHECK_NOT_CALLED_IN_SCOPE: scope=box function=toString

package foo

fun box(): String {
    var number = 3
    assertEquals("my age is 3", "my age is $number")
    return "OK"
}

