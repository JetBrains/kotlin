// CHECK_OPTIMIZED_JS
// WITH_STDLIB

import kotlin.reflect.KFunction0

fun interface I {
    fun run(): Int
}

class Box(val f: (Int) -> Int)

open class Base(val f: (Int) -> Int)

class Sub(f: (Int) -> Int) : Base(f)

// EXPECT_GENERATED_JS: function=invokeOnly$ref;Bound$boundInvoke$ref;dynamicInvoke$ref;kFunctionParam$ref;identity$ref;higherOrder$ref;storedField$ref;localVar$ref;samArgument$ref;samConstructor$ref;boxConstructor$ref;inheritedConstructor$ref;capturedInvoke$ref;nestedCapturedInvoke$ref;capturedAssignmentInvoke$ref;varargInvoke$ref;varargIndexed$ref;arrayOfInvoke$ref;spreadInvoke$ref;mixedSpreadInvoke$ref;forwardedVararg$ref;returnedArray$ref;objectField$ref;companionField$ref;objectVar$ref expect=unwrap.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=invokeOnly$ref;Bound$boundInvoke$ref;dynamicInvoke$ref;kFunctionParam$ref;identity$ref;higherOrder$ref;storedField$ref;localVar$ref;samArgument$ref;samConstructor$ref;boxConstructor$ref;inheritedConstructor$ref;capturedInvoke$ref;nestedCapturedInvoke$ref;capturedAssignmentInvoke$ref;varargInvoke$ref;varargIndexed$ref;arrayOfInvoke$ref;spreadInvoke$ref;mixedSpreadInvoke$ref;forwardedVararg$ref;returnedArray$ref;objectField$ref;companionField$ref;objectVar$ref expect=unwrap.es6.js TARGET_BACKENDS=JS_IR_ES6

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

fun capturedInvoke(x: Int) = x + 14

fun nestedCapturedInvoke(x: Int) = x + 15

fun capturedAssignmentInvoke(x: Int) = x + 16

fun varargInvoke(x: Int) = x + 17

fun varargIndexed(x: Int) = x + 18

fun arrayOfInvoke(x: Int) = x + 19

fun spreadInvoke(x: Int) = x + 20

fun mixedSpreadInvoke(x: Int) = x + 21

fun forwardedVararg(x: Int) = x + 22

fun returnedArray(x: Int) = x + 23

fun objectField(x: Int) = x + 40

fun companionField(x: Int) = x + 41

fun objectVar(x: Int) = x + 42

// A singleton has one instance, so field-based stores lose no precision.
object Registry {
    val f: (Int) -> Int = ::objectField
}

class WithCompanion {
    companion object {
        val f: (Int) -> Int = ::companionField
    }
}

object MutableRegistry {
    var f: (Int) -> Int = { it }
}

fun takeK(k: KFunction0<Int>) = k()

fun takeSam(i: I) = i.run()

fun <T> id(x: T): T = x

fun takeHof(g: (Int) -> Int) = g(10)

fun sumAll(vararg fs: (Int) -> Int): Int {
    var sum = 0
    for (f in fs) sum += f(10)
    return sum
}

fun invokeFirst(vararg fs: (Int) -> Int) = fs[0](10)

fun forwardAll(vararg fs: (Int) -> Int) = sumAll(*fs)

fun makeArray(): Array<(Int) -> Int> = arrayOf(::returnedArray)

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

    val captured = ::capturedInvoke
    val invokeCaptured = { captured(10) }
    if (invokeCaptured() != 24) return "fail captured"

    val nestedCaptured = ::nestedCapturedInvoke
    val invokeNestedCaptured = { { nestedCaptured(10) } }
    if (invokeNestedCaptured()() != 25) return "fail nested captured"

    var assignedCaptured: (Int) -> Int = { it }
    val assignCaptured = { assignedCaptured = ::capturedAssignmentInvoke }
    assignCaptured()
    if (assignedCaptured(10) != 26) return "fail captured assignment"

    if (sumAll(::varargInvoke) != 27) return "fail vararg"
    if (invokeFirst(::varargIndexed) != 28) return "fail vararg indexed"
    if (arrayOf<(Int) -> Int>(::arrayOfInvoke)[0](10) != 29) return "fail arrayOf"

    val spread = arrayOf<(Int) -> Int>(::spreadInvoke)
    if (sumAll(*spread) != 30) return "fail spread"

    val mixed = arrayOf<(Int) -> Int>(::mixedSpreadInvoke)
    if (sumAll({ it }, *mixed) != 41) return "fail mixed spread"

    if (forwardAll(::forwardedVararg) != 32) return "fail forwarded vararg"
    if (makeArray()[0](10) != 33) return "fail returned array"

    if (Registry.f(10) != 50) return "fail object"
    if (WithCompanion.f(10) != 51) return "fail companion"
    MutableRegistry.f = ::objectVar
    if (MutableRegistry.f(10) != 52) return "fail object var"

    return "OK"
}
