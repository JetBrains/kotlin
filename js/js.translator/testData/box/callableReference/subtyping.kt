// KT-35479

package foo

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KProperty

fun topLevelFun(x: Int): Int = x + 1
fun topLevelFunWithDefault(a: Int, b: Int = 1): Int = a + b

val topLevelVal: String = "hello"
var topLevelVar: String = "world"

suspend fun suspendTopLevelFun(x: Int): Int = x + 10
suspend fun suspendTopLevelFunWithDefault(a: Int, b: Int = 1): Int = a + b

fun twoParams(x: Int, y: Int): Int = x + y
fun threeParams(x: Int, y: Int, z: Int): Int = x + y + z

class A(val memberVal: String) {
    var memberVar: String = "mutable"
    fun memberFun(x: Int): Int = x * 2
}

fun box(): String {
    val lambda0 = { 42 }
    val lambda1 = { x: Int -> x + 1 }
    val lambda2 = { x: Int, y: Int -> x + y }

    if (lambda0 !is Function<*>)         return "Fail: lambda0 should be Function"
    if (lambda0 !is Function0<*>)        return "Fail: lambda0 should be Function0"
    if (lambda0 is Function1<*, *>)      return "Fail: lambda0 should NOT be Function1"
    if (lambda0 is KCallable<*>)         return "Fail: lambda0 should NOT be KCallable"
    if (lambda0 is KFunction<*>)         return "Fail: lambda0 should NOT be KFunction"
    if (lambda0 is KFunction0<*>)        return "Fail: lambda0 should NOT be KFunction0"
    if (lambda0 is KProperty<*>)         return "Fail: lambda0 should NOT be KProperty"

    if (lambda1 !is Function<*>)         return "Fail: lambda1 should be Function"
    if (lambda1 !is Function1<*, *>)     return "Fail: lambda1 should be Function1"
    if (lambda1 is Function0<*>)         return "Fail: lambda1 should NOT be Function0"
    if (lambda1 is Function2<*, *, *>)   return "Fail: lambda1 should NOT be Function2"
    if (lambda1 is KCallable<*>)         return "Fail: lambda1 should NOT be KCallable"
    if (lambda1 is KFunction<*>)         return "Fail: lambda1 should NOT be KFunction"
    if (lambda1 is KFunction1<*, *>)     return "Fail: lambda1 should NOT be KFunction1"
    if (lambda1 is KProperty<*>)         return "Fail: lambda1 should NOT be KProperty"

    if (lambda2 !is Function<*>)         return "Fail: lambda2 should be Function"
    if (lambda2 !is Function2<*, *, *>)  return "Fail: lambda2 should be Function2"
    if (lambda2 is Function1<*, *>)      return "Fail: lambda2 should NOT be Function1"
    if (lambda2 is Function3<*, *, *, *>) return "Fail: lambda2 should NOT be Function3"
    if (lambda2 is KCallable<*>)         return "Fail: lambda2 should NOT be KCallable"
    if (lambda2 is KFunction<*>)         return "Fail: lambda2 should NOT be KFunction"
    if (lambda2 is KFunction2<*, *, *>)  return "Fail: lambda2 should NOT be KFunction2"
    if (lambda2 is KProperty<*>)         return "Fail: lambda2 should NOT be KProperty"

    val funRef = ::topLevelFun

    if (funRef !is Function<*>)          return "Fail: ::topLevelFun should be Function"
    if (funRef !is Function1<*, *>)      return "Fail: ::topLevelFun should be Function1"
    if (funRef is Function0<*>)          return "Fail: ::topLevelFun should NOT be Function0"
    if (funRef is Function2<*, *, *>)    return "Fail: ::topLevelFun should NOT be Function2"
    if (funRef !is KCallable<*>)         return "Fail: ::topLevelFun should be KCallable"
    if (funRef !is KFunction<*>)         return "Fail: ::topLevelFun should be KFunction"
    if (funRef !is KFunction1<*, *>)     return "Fail: ::topLevelFun should be KFunction1"
    if (funRef is KFunction0<*>)         return "Fail: ::topLevelFun should NOT be KFunction0"
    if (funRef is KFunction2<*, *, *>)   return "Fail: ::topLevelFun should NOT be KFunction2"
    if (funRef is KProperty<*>)          return "Fail: ::topLevelFun should NOT be KProperty"
    if (funRef.name != "topLevelFun")    return "Fail: expected name 'topLevelFun', got '${funRef.name}'"
    if (funRef(1) != 2)                  return "Fail: expected topLevelFun(1) == 2"

    val twoRef = ::twoParams

    if (twoRef !is Function<*>)          return "Fail: ::twoParams should be Function"
    if (twoRef !is Function2<*, *, *>)   return "Fail: ::twoParams should be Function2"
    if (twoRef is Function1<*, *>)       return "Fail: ::twoParams should NOT be Function1"
    if (twoRef is Function3<*, *, *, *>) return "Fail: ::twoParams should NOT be Function3"
    if (twoRef !is KCallable<*>)         return "Fail: ::twoParams should be KCallable"
    if (twoRef !is KFunction<*>)         return "Fail: ::twoParams should be KFunction"
    if (twoRef !is KFunction2<*, *, *>)  return "Fail: ::twoParams should be KFunction2"
    if (twoRef is KFunction1<*, *>)      return "Fail: ::twoParams should NOT be KFunction1"
    if (twoRef is KFunction3<*, *, *, *>) return "Fail: ::twoParams should NOT be KFunction3"
    if (twoRef is KProperty<*>)          return "Fail: ::twoParams should NOT be KProperty"
    if (twoRef.name != "twoParams")      return "Fail: expected name 'twoParams', got '${twoRef.name}'"
    if (twoRef(2, 3) != 5)               return "Fail: expected twoParams(2, 3) == 5"

    val threeRef = ::threeParams

    if (threeRef !is Function<*>)            return "Fail: ::threeParams should be Function"
    if (threeRef !is Function3<*, *, *, *>)  return "Fail: ::threeParams should be Function3"
    if (threeRef is Function2<*, *, *>)      return "Fail: ::threeParams should NOT be Function2"
    if (threeRef !is KCallable<*>)           return "Fail: ::threeParams should be KCallable"
    if (threeRef !is KFunction<*>)           return "Fail: ::threeParams should be KFunction"
    if (threeRef !is KFunction3<*, *, *, *>) return "Fail: ::threeParams should be KFunction3"
    if (threeRef is KFunction2<*, *, *>)     return "Fail: ::threeParams should NOT be KFunction2"
    if (threeRef is KProperty<*>)            return "Fail: ::threeParams should NOT be KProperty"
    if (threeRef.name != "threeParams")      return "Fail: expected name 'threeParams', got '${threeRef.name}'"
    if (threeRef(1, 2, 3) != 6)              return "Fail: expected threeParams(1, 2, 3) == 6"

    val valRef = ::topLevelVal

    if (valRef !is Function<*>)          return "Fail: ::topLevelVal should be Function"
    if (valRef !is Function0<*>)         return "Fail: ::topLevelVal should be Function0"
    if (valRef is Function1<*, *>)       return "Fail: ::topLevelVal should NOT be Function1"
    if (valRef !is KCallable<*>)         return "Fail: ::topLevelVal should be KCallable"
    if (valRef !is KProperty<*>)         return "Fail: ::topLevelVal should be KProperty"
    if (valRef is KFunction<*>)          return "Fail: ::topLevelVal should NOT be KFunction"
    if (valRef is KFunction0<*>)         return "Fail: ::topLevelVal should NOT be KFunction0"
    if (valRef.name != "topLevelVal")    return "Fail: expected name 'topLevelVal', got '${valRef.name}'"
    if (valRef.get() != "hello")         return "Fail: expected topLevelVal == 'hello'"

    val varRef = ::topLevelVar

    if (varRef !is Function<*>)          return "Fail: ::topLevelVar should be Function"
    if (varRef !is Function0<*>)         return "Fail: ::topLevelVar should be Function0"
    if (varRef is Function1<*, *>)       return "Fail: ::topLevelVar should NOT be Function1"
    if (varRef !is KCallable<*>)         return "Fail: ::topLevelVar should be KCallable"
    if (varRef !is KProperty<*>)         return "Fail: ::topLevelVar should be KProperty"
    if (varRef is KFunction<*>)          return "Fail: ::topLevelVar should NOT be KFunction"
    if (varRef is KFunction0<*>)         return "Fail: ::topLevelVar should NOT be KFunction0"
    if (varRef.name != "topLevelVar")    return "Fail: expected name 'topLevelVar', got '${varRef.name}'"

    val memberFunRef = A::memberFun

    if (memberFunRef !is Function<*>)          return "Fail: A::memberFun should be Function"
    if (memberFunRef !is Function2<*, *, *>)   return "Fail: A::memberFun should be Function2"
    if (memberFunRef is Function1<*, *>)       return "Fail: A::memberFun should NOT be Function1"
    if (memberFunRef is Function3<*, *, *, *>) return "Fail: A::memberFun should NOT be Function3"
    if (memberFunRef !is KCallable<*>)         return "Fail: A::memberFun should be KCallable"
    if (memberFunRef !is KFunction<*>)         return "Fail: A::memberFun should be KFunction"
    if (memberFunRef !is KFunction2<*, *, *>)  return "Fail: A::memberFun should be KFunction2"
    if (memberFunRef is KFunction1<*, *>)      return "Fail: A::memberFun should NOT be KFunction1"
    if (memberFunRef is KProperty<*>)          return "Fail: A::memberFun should NOT be KProperty"
    if (memberFunRef.name != "memberFun")      return "Fail: expected name 'memberFun', got '${memberFunRef.name}'"

    val a = A("test")
    if (memberFunRef(a, 3) != 6)               return "Fail: expected A::memberFun(a, 3) == 6"

    val memberValRef = A::memberVal

    if (memberValRef !is Function<*>)          return "Fail: A::memberVal should be Function"
    if (memberValRef !is Function1<*, *>)      return "Fail: A::memberVal should be Function1"
    if (memberValRef is Function0<*>)          return "Fail: A::memberVal should NOT be Function0"
    if (memberValRef is Function2<*, *, *>)    return "Fail: A::memberVal should NOT be Function2"
    if (memberValRef !is KCallable<*>)         return "Fail: A::memberVal should be KCallable"
    if (memberValRef !is KProperty<*>)         return "Fail: A::memberVal should be KProperty"
    if (memberValRef is KFunction<*>)          return "Fail: A::memberVal should NOT be KFunction"
    if (memberValRef is KFunction1<*, *>)      return "Fail: A::memberVal should NOT be KFunction1"
    if (memberValRef.name != "memberVal")      return "Fail: expected name 'memberVal', got '${memberValRef.name}'"
    if (memberValRef.get(a) != "test")         return "Fail: expected A::memberVal.get(a) == 'test'"

    val memberVarRef = A::memberVar

    if (memberVarRef !is Function<*>)          return "Fail: A::memberVar should be Function"
    if (memberVarRef !is Function1<*, *>)      return "Fail: A::memberVar should be Function1"
    if (memberVarRef is Function0<*>)          return "Fail: A::memberVar should NOT be Function0"
    if (memberVarRef is Function2<*, *, *>)    return "Fail: A::memberVar should NOT be Function2"
    if (memberVarRef !is KCallable<*>)         return "Fail: A::memberVar should be KCallable"
    if (memberVarRef !is KProperty<*>)         return "Fail: A::memberVar should be KProperty"
    if (memberVarRef is KFunction<*>)          return "Fail: A::memberVar should NOT be KFunction"
    if (memberVarRef is KFunction1<*, *>)      return "Fail: A::memberVar should NOT be KFunction1"
    if (memberVarRef.name != "memberVar")      return "Fail: expected name 'memberVar', got '${memberVarRef.name}'"
    if (memberVarRef.get(a) != "mutable")      return "Fail: expected A::memberVar.get(a) == 'mutable'"

    val ctorRef = ::A

    if (ctorRef !is Function<*>)          return "Fail: ::A should be Function"
    if (ctorRef !is Function1<*, *>)      return "Fail: ::A should be Function1"
    if (ctorRef is Function0<*>)          return "Fail: ::A should NOT be Function0"
    if (ctorRef is Function2<*, *, *>)    return "Fail: ::A should NOT be Function2"
    if (ctorRef !is KCallable<*>)         return "Fail: ::A should be KCallable"
    if (ctorRef !is KFunction<*>)         return "Fail: ::A should be KFunction"
    if (ctorRef !is KFunction1<*, *>)     return "Fail: ::A should be KFunction1"
    if (ctorRef is KFunction0<*>)         return "Fail: ::A should NOT be KFunction0"
    if (ctorRef is KFunction2<*, *, *>)   return "Fail: ::A should NOT be KFunction2"
    if (ctorRef is KProperty<*>)          return "Fail: ::A should NOT be KProperty"
    if (ctorRef.name != "<init>")         return "Fail: expected ctor name '<init>', got '${ctorRef.name}'"

    val constructed = ctorRef("fromCtor")
    if (constructed.memberVal != "fromCtor") return "Fail: ctor ref produced wrong memberVal"

    val defaultFunRef = ::topLevelFunWithDefault 

    if (defaultFunRef !is Function<*>)             return "Fail: ::topLevelFunWithDefault should be Function"
    if (defaultFunRef !is Function2<*, *, *>)      return "Fail: ::topLevelFunWithDefault should be Function2"
    if (defaultFunRef is Function1<*, *>)          return "Fail: ::topLevelFunWithDefault should NOT be Function1"
    if (defaultFunRef is Function3<*, *, *, *>)    return "Fail: ::topLevelFunWithDefault should NOT be Function3"
    if (defaultFunRef !is KCallable<*>)            return "Fail: ::topLevelFunWithDefault should be KCallable"
    if (defaultFunRef !is KFunction<*>)            return "Fail: ::topLevelFunWithDefault should be KFunction"
     if (defaultFunRef !is KFunction1<*, *>)        return "Fail: ::topLevelFunWithDefault should be KFunction1"
    if (defaultFunRef !is KFunction2<*, *, *>)     return "Fail: ::topLevelFunWithDefault should be KFunction2"
    if (defaultFunRef is KFunction3<*, *, *, *>)   return "Fail: ::topLevelFunWithDefault should NOT be KFunction3"
    if (defaultFunRef is KProperty<*>)             return "Fail: ::topLevelFunWithDefault should NOT be KProperty"
    if (defaultFunRef !is ((Int, Int) -> Int))     return "Fail: ::topLevelFunWithDefault should be (Int, Int) -> Int"

    val suspendLambda0: suspend () -> Int         = { 42 }
    val suspendLambda1: suspend (Int) -> Int       = { x -> x + 1 }
    val suspendLambda2: suspend (Int, Int) -> Int  = { x, y -> x + y }

    if (suspendLambda0 !is Function<*>)            return "Fail: suspendLambda0 should be Function"
    if (suspendLambda0 !is Function1<*, *>)        return "Fail: suspendLambda0 should be Function1"
    if (suspendLambda0 !is (suspend () -> Int))    return "Fail: suspendLambda0 should be suspend () -> Int"
    if (suspendLambda0 is Function0<*>)            return "Fail: suspendLambda0 should NOT be Function0"
    if (suspendLambda0 is KCallable<*>)            return "Fail: suspendLambda0 should NOT be KCallable"
    if (suspendLambda0 is KFunction<*>)            return "Fail: suspendLambda0 should NOT be KFunction"
    if (suspendLambda0 is KProperty<*>)            return "Fail: suspendLambda0 should NOT be KProperty"

    if (suspendLambda1 !is Function<*>)            return "Fail: suspendLambda1 should be Function"
    if (suspendLambda1 !is (suspend (Int) -> Int)) return "Fail: suspendLambda1 should be suspend (Int) -> Int"
    if (suspendLambda1 !is Function2<*, *, *>)     return "Fail: suspendLambda1 should NOT be Function2"
    if (suspendLambda1 is Function1<*, *>)         return "Fail: suspendLambda1 should NOT be Function1"
    if (suspendLambda1 is KCallable<*>)            return "Fail: suspendLambda1 should NOT be KCallable"
    if (suspendLambda1 is KFunction<*>)            return "Fail: suspendLambda1 should NOT be KFunction"
    if (suspendLambda1 is KProperty<*>)            return "Fail: suspendLambda1 should NOT be KProperty"

    if (suspendLambda2 !is Function<*>)                    return "Fail: suspendLambda2 should be Function"
    if (suspendLambda2 !is (suspend (Int, Int) -> Int))    return "Fail: suspendLambda2 should be suspend (Int, Int) -> Int"
    if (suspendLambda2 !is Function3<*, *, *, *>)          return "Fail: suspendLambda2 should be Function3"
    if (suspendLambda2 is Function2<*, *, *>)              return "Fail: suspendLambda2 should NOT be Function2"
    if (suspendLambda2 is KCallable<*>)                    return "Fail: suspendLambda2 should NOT be KCallable"
    if (suspendLambda2 is KFunction<*>)                    return "Fail: suspendLambda2 should NOT be KFunction"
    if (suspendLambda2 is KProperty<*>)                    return "Fail: suspendLambda2 should NOT be KProperty"

    val suspendFunRef = ::suspendTopLevelFun

    if (suspendFunRef !is (suspend (Int) -> Int))          return "Fail: ::suspendTopLevelFun should be suspend (Int) -> Int"

    if (suspendFunRef !is Function<*>)              return "Fail: ::suspendTopLevelFun should be Function"
    if (suspendFunRef !is Function2<*, *, *>)          return "Fail: ::suspendTopLevelFun should be Function2"
    if (suspendFunRef is Function0<*>)              return "Fail: ::suspendTopLevelFun should NOT be Function0"
    if (suspendFunRef is Function1<*, *>)        return "Fail: ::suspendTopLevelFun should NOT be Function1"

    if (suspendFunRef !is KCallable<*>)             return "Fail: ::suspendTopLevelFun should be KCallable"
    if (suspendFunRef !is KFunction<*>)             return "Fail: ::suspendTopLevelFun should be KFunction"
    if (suspendFunRef !is KFunction2<*, *, *>)      return "Fail: ::suspendTopLevelFun should be KFunction2"
    if (suspendFunRef is KFunction0<*>)             return "Fail: ::suspendTopLevelFun should NOT be KFunction0"
    if (suspendFunRef is KFunction1<*, *>)       return "Fail: ::suspendTopLevelFun should NOT be KFunction1"

    if (suspendFunRef is KProperty<*>)              return "Fail: ::suspendTopLevelFun should NOT be KProperty"

    val suspendDefaultFunRef = ::suspendTopLevelFunWithDefault

    if (suspendDefaultFunRef !is Function<*>)             return "Fail: ::suspendTopLevelFunWithDefault should be Function"
    if (suspendDefaultFunRef !is Function3<*, *, *, *>)   return "Fail: ::suspendTopLevelFunWithDefault should be Function2"
    if (suspendDefaultFunRef is Function2<*, *, *>)          return "Fail: ::suspendTopLevelFunWithDefault should NOT be Function2"
    if (suspendDefaultFunRef is Function1<*, *>)    return "Fail: ::suspendTopLevelFunWithDefault should NOT be Function1"
    if (suspendDefaultFunRef !is KCallable<*>)            return "Fail: ::suspendTopLevelFunWithDefault should be KCallable"
    if (suspendDefaultFunRef !is KFunction<*>)            return "Fail: ::suspendTopLevelFunWithDefault should be KFunction"
    if (suspendDefaultFunRef !is KFunction2<*, *, *>)        return "Fail: ::suspendTopLevelFunWithDefault should be KFunction1"
    if (suspendDefaultFunRef !is KFunction3<*, *, *, *>)     return "Fail: ::suspendTopLevelFunWithDefault should be KFunction2"
    if (suspendDefaultFunRef is KFunction1<*, *>)   return "Fail: ::suspendTopLevelFunWithDefault should NOT be KFunction3"
    if (suspendDefaultFunRef is KProperty<*>)             return "Fail: ::suspendTopLevelFunWithDefault should NOT be KProperty"
    if (suspendDefaultFunRef !is (suspend (Int, Int) -> Int))     return "Fail: ::suspendTopLevelFunWithDefault should be (Int, Int) -> Int"

    return "OK"
}
