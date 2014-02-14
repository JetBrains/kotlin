package foo

import js.*

native
fun paramCount(vararg a: Int): Int = js.noImpl

native("paramCount")
fun anotherParamCount(vararg a: Int): Int = js.noImpl

// test spread operator
fun count(vararg a: Int) = paramCount(*a)

// test spread operator
fun anotherCount(vararg a: Int) = anotherParamCount(*a)

native
fun test3(bar: Bar, dummy: Int, vararg args: Int): Boolean = js.noImpl

native
fun Bar.test2(order: Int, dummy: Int, vararg args: Int): Boolean = js.noImpl

native
class Bar(val size: Int, order: Int = 0) {
    fun test(order: Int, dummy: Int, vararg args: Int): Boolean = js.noImpl
    class object {
        fun startNewTest(): Boolean = js.noImpl
        var hasOrderProblem: Boolean = false
    }
}

native
object obj {
    fun test(size: Int, vararg args: Int): Boolean = js.noImpl
}

fun spreadInMethodCall(size: Int, vararg args: Int) = Bar(size).test(0, 1, *args)

fun spreadInObjectMethodCall(size: Int, vararg args: Int) = obj.test(size, *args)

fun spreadInMethodCallWithReceiver(size: Int, vararg args: Int) = Bar(size).test2(0, 1, *args)

fun spreadInPackageMethodCall(size: Int, vararg args: Int) = test3(Bar(size), 1, *args)

native
fun testNativeVarargWithFunLit(vararg args: Int, f: (a: IntArray) -> Boolean): Boolean = js.noImpl

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

    return "OK"
}