// EXPECTED_REACHABLE_NODES: 491
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

fun run(arg1: A, arg2: String, funRef:A.(String) -> Unit): Unit {
    return arg1.funRef(arg2)
}

class A {
    var result = "Fail"
}

fun A.foo(newResult: String) {
    result = newResult
}

fun box(): String {
    val a = A()
    val x = A::foo
    x(a, "OK")

    if (a.result != "OK") return a.result

    // TODO: uncomment when KT-13312 gets fixed
    /*
    val a1 = A()
    run(a1, "OK", A::foo)
    if (a1.result != "OK) return a1.result
    */

    return "OK"
}
