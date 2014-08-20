// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun run(arg1: A, funRef:A.() -> Unit): Unit {
    return arg1.funRef()
}

class A {
    var result = "Fail"
}

fun A.foo() {
    result = "OK"
}

fun box(): String {
    val a = A()
    val x = A::foo
    a.x()

    if (a.result != "OK") return a.result

    val a1 = A()
    run(a1, A::foo)
    return a.result
}
