package foo

// CHECK_CONTAINS_NO_CALLS: sumEven

inline fun filteredReduce(a: Array<Int>, inline predicate: (Int) -> Boolean, inline reduceFun: (Int, Int) -> Int): Int {
    var result = 0

    for (element in a) {
        val satisfyPred = predicate(element)
        if (satisfyPred) {
            result = reduceFun(result, element)
        }
    }

    return result
}

fun sumEven(a: Array<Int>): Int {
    return filteredReduce(a, {(x) -> x % 2 == 0}, {(x, y) -> x + y})
}

fun box(): String {
    assertEquals(0, sumEven(array()))
    assertEquals(6, sumEven(array(1,2,3,4)))
    assertEquals(12, sumEven(array(1,2,3,4,6,7,9)))

    return "OK"
}