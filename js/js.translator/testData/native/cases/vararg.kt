package foo

native
fun paramCount(vararg a: Int): Int = noImpl

native("paramCount")
fun anotherParamCount(vararg a: Int): Int = noImpl

native("paramCount")
fun genericParamCount<T>(vararg a: T): Int = noImpl

// test spread operator
fun count(vararg a: Int) = paramCount(*a)

// test spread operator
fun anotherCount(vararg a: Int) = anotherParamCount(*a)

native
fun test3(bar: Bar, dummy: Int, vararg args: Int): Boolean = noImpl

native
fun Bar.test2(order: Int, dummy: Int, vararg args: Int): Boolean = noImpl

native
class Bar(val size: Int, order: Int = 0) {
    fun test(order: Int, dummy: Int, vararg args: Int): Boolean = noImpl
    default object {
        fun startNewTest(): Boolean = noImpl
        var hasOrderProblem: Boolean = false
    }
}

native
object obj {
    fun test(size: Int, vararg args: Int): Boolean = noImpl
}

fun spreadInMethodCall(size: Int, vararg args: Int) = Bar(size).test(0, 1, *args)

fun spreadInObjectMethodCall(size: Int, vararg args: Int) = obj.test(size, *args)

fun spreadInMethodCallWithReceiver(size: Int, vararg args: Int) = Bar(size).test2(0, 1, *args)

fun spreadInPackageMethodCall(size: Int, vararg args: Int) = test3(Bar(size), 1, *args)

native
fun testNativeVarargWithFunLit(vararg args: Int, f: (a: IntArray) -> Boolean): Boolean = noImpl

fun testSpreadOperatorWithSafeCall(a: Bar?, expected: Boolean?, vararg args: Int): Boolean {
    return a?.test(0, 1, *args) == expected
}

fun testSpreadOperatorWithSureCall(a: Bar?, vararg args: Int): Boolean {
    return a!!.test(0, 1, *args)
}

fun testCallOrder(vararg args: Int) =
        Bar.startNewTest() &&
        Bar(args.size, 0).test(1, 1, *args) && Bar(args.size, 2).test(3, 1, *args) &&
        !Bar.hasOrderProblem

native
fun sumOfParameters(x: Int, y: Int, vararg a: Int): Int = noImpl

native
fun sumFunValuesOnParameters(x: Int, y: Int, vararg a: Int, f: (Int) -> Int): Int = noImpl

native
fun idArrayVarArg<T>(vararg a: Array<T>): Array<T> = noImpl

fun box(): String {
    if (paramCount() != 0)
        return "failed when call native function without args"

    if (paramCount(1, 2, 3) != 3)
        return "failed when call native function with some args"

    if (anotherParamCount(1, 2, 3) != 3)
        return "failed when call native function with some args witch declareted with custom name"

    if (count() != 0)
        return "failed when call native function without args using spread operator"

    if (count(1, 1, 1, 1) != 4)
        return "failed when call native function with some args using spread operator"

    if (anotherCount(1, 2, 3) != 3)
        return "failed when call native function with some args using spread operator witch declareted with custom name"

    if (!Bar(5).test(0, 1, 1, 2, 3, 4, 5))
        return "failed when call method with some args"

    if (!spreadInMethodCall(2, 1, 2))
        return "failed when call method using spread operator"

    if (!(obj.test(5, 1, 2, 3, 4, 5)))
        return "failed when call method of object"

    if (!(spreadInObjectMethodCall(2, 1, 2)))
        return "failed when call method of object using spread operator"

    if (!spreadInMethodCallWithReceiver(2, 1, 2))
        return "failed when call method using spread operator with receiver"

    if (!spreadInPackageMethodCall(2, 1, 2))
        return "failed when call package method using spread operator"

    if (!(testNativeVarargWithFunLit(1, 2, 3) { args -> args.size == 3 }))
        return "failed when call native function with vararg and fun literal"

    if (!(testSpreadOperatorWithSafeCall(null, null)))
        return "failed when test spread operator with SafeCall (?.) using null receiver"

    if (!(testSpreadOperatorWithSafeCall(Bar(3), true, 1, 2, 3)))
        return "failed when test spread operator with SafeCall (?.)"

    if (!(testSpreadOperatorWithSureCall(Bar(3), 1, 2, 3)))
        return "failed when test spread operator with SureCall (!!)"

    if (!(testCallOrder()))
        return "failed when test calling order when using spread operator without args"

    if (!(testCallOrder(1, 2, 3, 4)))
        return "failed when test calling order when using spread operator with some args"

    val baz: Bar? = Bar(1)
    if (!(baz!!)?.test(0, 1, 1))
        return "failed when combined SureCall and SafeCall, maybe we lost cached expression"

    val a = array(1, 2)
    assertEquals(2, genericParamCount(*a))
    assertEquals(7, genericParamCount(1, *a, *a, 1, 2))

    assertEquals(45, sumOfParameters(1, 2, 3, 4, 5, 6, 7, 8, 9))
    assertEquals(45, sumOfParameters(1, 2, *intArray(3, 4, 5, 6, 7, 8, 9)))
    assertEquals(45, sumOfParameters(1, 2, 3, 4, *intArray(5, 6, 7, 8, 9)))
    assertEquals(90, sumFunValuesOnParameters(1, 2, 3, 4, 5, 6, 7, 8, 9) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArray(3, 4, 5, 6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, 3, 4, *intArray(5, 6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArray(3, 4, 5, 6, 7), 8, 9) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArray(3, 4, 5), *intArray(6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArray(3, 4), 5, 6, *intArray(7, 8, 9)) { 2*it })

    assertEquals(2, idArrayVarArg(array(1), *array(array(2, 3, 4))).size())
    assertEquals(3, idArrayVarArg(array(1, 2), *array(array(3, 4), array(5, 6))).size())
    assertEquals(6, idArrayVarArg(array(1, 2), *array(array(3, 4), array(5, 6)), array(7), *array(array(8, 9), array(10, 11))).size())

    return "OK"
}
