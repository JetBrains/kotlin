// EXPECTED_REACHABLE_NODES: 492
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

fun run(arg1: A, arg2: String, funRef:A.(String) -> String): String {
    return arg1.funRef(arg2)
}

class A

fun A.foo(result: String) = result

fun box(): String {
    val x = A::foo
    var r = x(A(), "OK")
    if (r != "OK") return r

    r = run(A(), "OK", A::foo)
    return "OK"
}
