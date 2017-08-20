// EXPECTED_REACHABLE_NODES: 991
package foo

fun box(): String {
    var sum = 0
    val adder = { a: Int -> sum += a }
    adder(3)
    adder(2)

    if (sum != 5) return "fail: $sum"
    return "OK"
}