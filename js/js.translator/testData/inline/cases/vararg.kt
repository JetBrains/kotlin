package foo


// CHECK_CONTAINS_NO_CALLS: test1
// CHECK_CONTAINS_NO_CALLS: test2

inline fun sum(vararg nums: Int): Int {
    var result = 0

    for (num in nums) {
        result += num
    }

    return result
}

fun test1(): Int {
    return sum()
}

fun test2(): Int {
    return sum(1,2,3)
}

fun box(): String {
    assertEquals(0, test1())
    assertEquals(6, test2())

    return "OK"
}