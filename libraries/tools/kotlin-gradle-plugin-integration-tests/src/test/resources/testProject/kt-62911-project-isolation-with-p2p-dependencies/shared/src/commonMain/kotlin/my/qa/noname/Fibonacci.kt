package my.qa.noname

/**
 * Returns a list of Fibonacci numbers up to the specified count.
 *
 * @param count The number of Fibonacci numbers to generate.
 * @return A list of Fibonacci numbers.
 * @throws IllegalArgumentException if count is less than zero.
 */
fun getFibonacciNumbers(count: Int): List<Int> {
    require(count >= 0)
    val result = mutableListOf<Int>()

    if (count == 0) return result

    var t1 = 0
    var t2 = 1
    for (i in 1..count) {
        result.add(t1)
        val sum = t1 + t2
        t1 = t2
        t2 = sum
    }

    return result
}