// EXPECTED_REACHABLE_NODES: 991
package foo

fun box(): String {
    var sum = 0
    val addFive = { a: Int -> a + 5 }
    sum = addFive(sum)
    if (sum != 5) return "fail: $sum"
    return "OK"
}