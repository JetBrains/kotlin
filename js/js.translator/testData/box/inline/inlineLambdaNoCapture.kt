// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: sumEven

internal inline fun filteredReduce(a: Array<Int>, predicate: (Int) -> Boolean, reduceFun: (Int, Int) -> Int): Int {
    var result = 0

    for (element in a) {
        val satisfyPred = predicate(element)
        if (satisfyPred) {
            result = reduceFun(result, element)
        }
    }

    return result
}

internal fun sumEven(a: Array<Int>): Int {
    return filteredReduce(a, { x -> x % 2 == 0}, { x, y -> x + y})
}

fun box(): String {
    assertEquals(0, sumEven(arrayOf()))
    assertEquals(6, sumEven(arrayOf(1,2,3,4)))
    assertEquals(12, sumEven(arrayOf(1,2,3,4,6,7,9)))

    return "OK"
}