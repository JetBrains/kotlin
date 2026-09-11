// CHECK_OPTIMIZED_JS
// WITH_STDLIB

import kotlin.reflect.KFunction0

fun interface I {
    fun run(): Int
}

class Box(val f: (Int) -> Int)

open class Base(val f: (Int) -> Int)

class Sub(f: (Int) -> Int) : Base(f)

// EXPECT_GENERATED_JS: function=invokeOnly$ref;Bound$boundInvoke$ref;dynamicInvoke$ref;kFunctionParam$ref;identity$ref;higherOrder$ref;storedField$ref;localVar$ref;samArgument$ref;samConstructor$ref;boxConstructor$ref;inheritedConstructor$ref expect=unwrap.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=invokeOnly$ref;Bound$boundInvoke$ref;dynamicInvoke$ref;kFunctionParam$ref;identity$ref;higherOrder$ref;storedField$ref;localVar$ref;samArgument$ref;samConstructor$ref;boxConstructor$ref;inheritedConstructor$ref expect=unwrap.es6.js TARGET_BACKENDS=JS_IR_ES6

fun invokeOnly(x: Int) = x + 1

class Bound(val n: Int) {
    fun boundInvoke(x: Int) = n + x
}

fun dynamicInvoke(x: Int) = x + 2

fun kFunctionParam() = 4

fun identity(x: Int) = x + 5

fun higherOrder(x: Int) = x + 6

fun storedField(x: Int) = x + 7

fun localVar(x: Int) = x + 8

fun samArgument() = 11

fun samConstructor() = 21

fun boxConstructor(x: Int) = x + 12

fun inheritedConstructor(x: Int) = x + 13

fun takeK(k: KFunction0<Int>) = k()

fun takeSam(i: I) = i.run()

fun <T> id(x: T): T = x

fun takeHof(g: (Int) -> Int) = g(10)

class Holder {
    var f: (Int) -> Int = ::storedField
}

fun box(): String {
    val f = ::invokeOnly
    if (f(10) != 11) return "fail invoke"

    val b = Bound(3)
    val bound = b::boundInvoke
    if (bound(4) != 7) return "fail bound"

    val d: dynamic = ::dynamicInvoke
    if (d(10) != 12) return "fail dynamic"

    if (takeK(::kFunctionParam) != 4) return "fail kfun"
    if (id(::identity)(10) != 15) return "fail id"
    if (takeHof(::higherOrder) != 16) return "fail hof"
    if (Holder().f(10) != 17) return "fail field"

    var p = ::localVar
    if (p(10) != 18) return "fail local"

    if (takeSam(::samArgument) != 11) return "fail sam argument"

    val i = I(::samConstructor)
    if (i.run() != 21) return "fail sam constructor"

    if (Box(::boxConstructor).f(10) != 22) return "fail box constructor"
    if (Sub(::inheritedConstructor).f(10) != 23) return "fail inherited constructor"

    return "OK"
}
