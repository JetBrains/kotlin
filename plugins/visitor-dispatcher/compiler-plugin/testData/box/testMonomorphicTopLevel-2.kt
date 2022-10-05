// DUMP_IR

import org.jetbrains.kotlin.specialization.Monomorphic

abstract class A {
    abstract fun f()
}
class B: A() {
    override fun f() {}
}

// 1. Type substitution [T -> X: A] in all type holders
fun <@Monomorphic T: A> testId(x: T): T {
    return x
}

// 2. Refinement of member call with monomorphic type parameter
// If type(E) = T, T <: A then E.call(...) is resolved to E::call instead of A::call
fun <@Monomorphic T: A> testRefinementOfMemberCallWithMonomorphicType(x: T) {
    x.f()
}

// 3. Refinement of monomorphic calee
fun <@Monomorphic T1: A> testRefinementOfMonomorphicFunction(x: T1) {
    monomorphicCallee(x, x) // (T2 -> T1, T3 -> T1) * (T1 -> B)
}

fun <@Monomorphic T2: A, @Monomorphic T3: A> monomorphicCallee(x: T2, y: T3) {}

fun box(): String {
    // check call sites reifinement
    testId(B())
    testRefinementOfMemberCallWithMonomorphicType(B())
    testRefinementOfMonomorphicFunction(B())
    monomorphicCallee(B(), B())

    return "OK"
}