package foo

fun box(): Boolean {
    var sum = 0
    val addFive = { a: Int -> a + 5 }
    sum = addFive(sum)
    return sum == 5
}