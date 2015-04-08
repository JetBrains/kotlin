package foo

fun box(): Boolean {
    var sum = 0
    val adder = { a: Int -> sum += a }
    adder(3)
    adder(2)
    return sum == 5
}