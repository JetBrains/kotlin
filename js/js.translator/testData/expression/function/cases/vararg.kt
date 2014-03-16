package foo

fun testSize(expectedSize: Int, vararg i: Int): Boolean {
    return (i.size == expectedSize)
}

fun testSum(expectedSum: Int, vararg i: Int): Boolean {
    var sum = 0
    for (j in i) {
        sum += j
    }

    return (expectedSum == sum)
}

fun testSpreadOperator(vararg args: Int): Boolean {
    var sum = 0;
    for (a in args) sum += a

    return testSize(args.size, *args) && testSum(sum, *args)
}

class Bar(val size: Int, val sum: Int) {
    fun test(vararg args: Int) = testSize(size, *args) && testSum(sum, *args)
}

object obj {
    fun test(size: Int, sum: Int, vararg args: Int) = testSize(size, *args) && testSum(sum, *args)
}

fun spreadInMethodCall(size: Int, sum: Int, vararg args: Int) = Bar(size, sum).test(*args)

fun spreadInObjectMethodCall(size: Int, sum: Int, vararg args: Int) = obj.test(size, sum, *args)

fun testVarargWithFunLit(vararg args: Int, f: (a: IntArray) -> Boolean): Boolean = f(args)

fun box(): String {
    if (!testSize(0))
        return "wrong vararg size when call function without args"

    if (!testSum(0))
        return "wrong vararg sum (arguments) when call function without args"

    if (!testSize(6, 1, 1, 1, 2, 3, 4))
        return "wrong vararg size when call function with some args (1)"

    if (!testSum(30, 10, 20, 0))
        return "wrong vararg sum (arguments) when call function with some args (1)"

    if (!testSpreadOperator(30, 10, 20, 0))
        return "failed when call function using spread operator"

    if (!Bar(3, 30).test(10, 20, 0))
        return "failed when call method"

    if (!spreadInMethodCall(2, 3, 1, 2))
        return "failed when call method using spread operator"

    if (!obj.test(5, 15, 1, 2, 3, 4, 5))
        return "failed when call method of object"

    if (!spreadInObjectMethodCall(2, 3, 1, 2))
        return "failed when call method of object using spread operator"

    if (!testVarargWithFunLit(1, 2, 3) { args -> args.size == 3 })
        return "failed when call function with vararg and fun literal"

    return "OK"
}