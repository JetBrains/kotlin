// LANGUAGE_VERSION: 1.3
// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1566
package foo

fun testSize(expectedSize: Int, vararg i: UInt): Boolean {
    return (i.size == expectedSize)
}

fun testSum(expectedSum: UInt, vararg i: UInt): Boolean {
    var sum = 0u
    for (j in i) {
        sum += j
    }

    return (expectedSum == sum)
}

fun testSpreadOperator(vararg args: UInt): Boolean {
    var sum = 0u
    for (a in args) sum += a

    return testSize(args.size, *args) && testSum(sum, *args)
}

class Bar(val size: Int, val sum: UInt) {
    fun test(vararg args: UInt) = testSize(size, *args) && testSum(sum, *args)
}

object obj {
    fun test(size: Int, sum: UInt, vararg args: UInt) = testSize(size, *args) && testSum(sum, *args)
}

fun spreadInMethodCall(size: Int, sum: UInt, vararg args: UInt) = Bar(size, sum).test(*args)

fun spreadInObjectMethodCall(size: Int, sum: UInt, vararg args: UInt) = obj.test(size, sum, *args)

fun testVarargWithFunLit(vararg args: UInt, f: (a: UIntArray) -> Boolean): Boolean = f(args)

fun <T> idVarArgs(vararg a: T) = a

fun <T> idArrayVarArg(vararg a: Array<T>) = a

fun sumFunValuesOnParameters(x: UInt, y: UInt, vararg a: UInt, f: (UInt) -> UInt): UInt {
    var result = f(x) + f(y)
    for(u in a) {
        result += f(u)
    }
    return result
}

fun box(): String {
    if (!testSize(0))
        return "wrong vararg size when call function without args"

    if (!testSum(0u))
        return "wrong vararg sum (arguments) when call function without args"

    if (!testSize(6, 1u, 1u, 1u, 2u, 3u, 4u))
        return "wrong vararg size when call function with some args (1)"

    if (!testSum(30u, 10u, 20u, 0u))
        return "wrong vararg sum (arguments) when call function with some args (1)"

    if (!testSpreadOperator(30u, 10u, 20u, 0u))
        return "failed when call function using spread operator"

    if (!Bar(3, 30u).test(10u, 20u, 0u))
        return "failed when call method"

    if (!spreadInMethodCall(2, 3u, 1u, 2u))
        return "failed when call method using spread operator"

    if (!obj.test(5, 15u, 1u, 2u, 3u, 4u, 5u))
        return "failed when call method of object"

    if (!spreadInObjectMethodCall(2, 3u, 1u, 2u))
        return "failed when call method of object using spread operator"

    if (!testVarargWithFunLit(1u, 2u, 3u) { args -> args.size == 3 })
        return "failed when call function with vararg and fun literal"

    val a = arrayOf(1u, 2u, 3u)
    val b = arrayOf(4u, 5u)

    assertEquals(5, arrayOf(*a, *b).size)
    assertEquals(8, arrayOf(10u, *a, 20u,  *b, 30u).size)

    assertEquals(5, idVarArgs(*a, *b).size)
    assertEquals(8, idVarArgs(10u, *a, 20u,  *b, 30u).size)

    assertEquals(9, arrayOf(1u, *a, *a, 1u, 2u).size)
    assertEquals(9, idVarArgs(1u, *a, *a, 1u, 2u).size)

    assertEquals(9, arrayOf(1u, *a, *arrayOf(1u, 2u, 3u), 1u, 2u).size)
    assertEquals(9, idVarArgs(1u, *a, *arrayOf(1u, 2u, 3u), 1u, 2u).size)

    assertEquals(90u, sumFunValuesOnParameters(1u, 2u, 3u, 4u, 5u, 6u, 7u, 8u, 9u) { 2u*it })
    assertEquals(90u, sumFunValuesOnParameters(1u, 2u, *uintArrayOf(3u, 4u, 5u, 6u, 7u, 8u, 9u)) { 2u*it })
    assertEquals(90u, sumFunValuesOnParameters(1u, 2u, 3u, 4u, *uintArrayOf(5u, 6u, 7u, 8u, 9u)) { 2u*it })
    assertEquals(90u, sumFunValuesOnParameters(1u, 2u, *uintArrayOf(3u, 4u, 5u, 6u, 7u), 8u, 9u) { 2u*it })
    assertEquals(90u, sumFunValuesOnParameters(1u, 2u, *uintArrayOf(3u, 4u, 5u), *uintArrayOf(6u, 7u, 8u, 9u)) { 2u*it })
    assertEquals(90u, sumFunValuesOnParameters(1u, 2u, *uintArrayOf(3u, 4u), 5u, 6u, *uintArrayOf(7u, 8u, 9u)) { 2u*it })

    assertEquals(2, idArrayVarArg(arrayOf(1u), *arrayOf(arrayOf(2u, 3u, 4u))).size)
    assertEquals(3, idArrayVarArg(arrayOf(1u, 2u), *arrayOf(arrayOf(3u, 4u), arrayOf(5u, 6u))).size)
    assertEquals(6, idArrayVarArg(arrayOf(1u, 2u), *arrayOf(arrayOf(3u, 4u), arrayOf(5u, 6u)), arrayOf(7u), *arrayOf(arrayOf(8u, 9u), arrayOf(10u, 11u))).size)

    val c = arrayOf(*a)
    assertFalse(a === c, "Spread operator should copy its argument")

    return "OK"
}