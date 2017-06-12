// EXPECTED_REACHABLE_NODES: 501
package foo

// CHECK_CONTAINS_NO_CALLS: maxBySquare except=imul

internal data class Result(var value: Int = 0, var invocationCount: Int = 0)

internal inline fun maxBy(a: Array<Int>, keyFun: (Int) -> Int): Int {
    var maxVal = a[0]
    var maxKey = keyFun(maxVal)

    for (element in a) {
        val key = keyFun(element)

        if (key > maxKey) {
            maxVal = element
            maxKey = key
        }
    }

    return maxVal
}

internal fun maxBySquare(a: Array<Int>, r: Result): Result {
    var invocationCount = 0
    val maxVal = maxBy(a, { x -> invocationCount++; x * x;})

    r.value = maxVal
    r.invocationCount = invocationCount

    return r
}

fun box(): String {
    var r1 = maxBySquare(arrayOf(1,2,3,4,5), Result())
    assertEquals(Result(5, 6), r1)

    var r2 = maxBySquare(arrayOf(-5,1,2,3,4), Result())
    assertEquals(Result(-5, 6), r2)

    return "OK"
}