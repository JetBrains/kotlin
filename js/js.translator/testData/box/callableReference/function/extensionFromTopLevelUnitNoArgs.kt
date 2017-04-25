// EXPECTED_REACHABLE_NODES: 491
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A {
    var result = "Fail"
}

fun A.foo() {
    result = "OK"
}

fun box(): String {
    val a = A()
    val x = A::foo
    x(a)

    return a.result
}
