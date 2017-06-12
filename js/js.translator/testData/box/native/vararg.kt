// EXPECTED_REACHABLE_NODES: 530
package foo

external fun paramCount(vararg a: Int): Int = definedExternally

@JsName("paramCount")
external fun anotherParamCount(vararg a: Int): Int = definedExternally

@JsName("paramCount")
external fun <T> genericParamCount(vararg a: T): Int = definedExternally

// test spread operator
fun count(vararg a: Int) = paramCount(*a)

// test spread operator
fun anotherCount(vararg a: Int) = anotherParamCount(*a)

external fun test3(bar: Bar, dummy: Int, vararg args: Int): Boolean = definedExternally

external class Bar(size: Int, order: Int = definedExternally) {
    val size: Int
    fun test(order: Int, dummy: Int, vararg args: Int): Boolean = definedExternally
    companion object {
        fun startNewTest(): Boolean = definedExternally
        var hasOrderProblem: Boolean = definedExternally
    }
}

external object obj {
    fun test(size: Int, vararg args: Int): Boolean = definedExternally
}

fun spreadInMethodCall(size: Int, vararg args: Int) = Bar(size).test(0, 1, *args)

fun spreadInObjectMethodCall(size: Int, vararg args: Int) = obj.test(size, *args)

fun spreadInPackageMethodCall(size: Int, vararg args: Int) = test3(Bar(size), 1, *args)

external fun testNativeVarargWithFunLit(vararg args: Int, f: (a: IntArray) -> Boolean): Boolean = definedExternally

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

external fun sumOfParameters(x: Int, y: Int, vararg a: Int): Int = definedExternally

external fun sumFunValuesOnParameters(x: Int, y: Int, vararg a: Int, f: (Int) -> Int): Int = definedExternally

external fun <T> idArrayVarArg(vararg a: Array<T>): Array<T> = definedExternally

@JsName("paramCount")
external fun oneMoreParamCount(before: IntArray, vararg middle: Int, after: IntArray): Int

@JsName("paramCount")
external fun <T> oneMoreGenericParamCount(before: Array<T>, vararg middle: T, after: Array<T>): Int

fun box(): String {
    if (paramCount() != 0)
        return "failed when call native function without args"

    if (paramCount(1) != 1) return "failed when call native function with single vararg"

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

    if (!Bar(1).test(0, 1, 1))
        return "failed when call method with single arg"

    if (!spreadInMethodCall(2, 1, 2))
        return "failed when call method using spread operator"

    if (!(obj.test(5, 1, 2, 3, 4, 5)))
        return "failed when call method of object"

    if (!(spreadInObjectMethodCall(2, 1, 2)))
        return "failed when call method of object using spread operator"

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

    val a = arrayOf(1, 2)
    assertEquals(2, genericParamCount(*a))
    assertEquals(7, genericParamCount(1, *a, *a, 1, 2))

    assertEquals(45, sumOfParameters(1, 2, 3, 4, 5, 6, 7, 8, 9))
    assertEquals(45, sumOfParameters(1, 2, *intArrayOf(3, 4, 5, 6, 7, 8, 9)))
    assertEquals(45, sumOfParameters(1, 2, 3, 4, *intArrayOf(5, 6, 7, 8, 9)))
    assertEquals(90, sumFunValuesOnParameters(1, 2, 3, 4, 5, 6, 7, 8, 9) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArrayOf(3, 4, 5, 6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, 3, 4, *intArrayOf(5, 6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArrayOf(3, 4, 5, 6, 7), 8, 9) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArrayOf(3, 4, 5), *intArrayOf(6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArrayOf(3, 4), 5, 6, *intArrayOf(7, 8, 9)) { 2*it })

    assertEquals(2, idArrayVarArg(arrayOf(1), *arrayOf(arrayOf(2, 3, 4))).size)
    assertEquals(3, idArrayVarArg(arrayOf(1, 2), *arrayOf(arrayOf(3, 4), arrayOf(5, 6))).size)
    assertEquals(6, idArrayVarArg(arrayOf(1, 2), *arrayOf(arrayOf(3, 4), arrayOf(5, 6)), arrayOf(7), *arrayOf(arrayOf(8, 9), arrayOf(10, 11))).size)

    assertEquals(6, oneMoreParamCount(intArrayOf(1, 2), 3, *intArrayOf(4, 5), 6, after = intArrayOf(7, 8)))
    assertEquals(6, oneMoreGenericParamCount(arrayOf("1", "2"), "3", *arrayOf("4", "5"), "6", after = arrayOf("7", "8")))
    return "OK"
}