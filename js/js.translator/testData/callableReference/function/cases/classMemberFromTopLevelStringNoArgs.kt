// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun run(arg1: A, funRef:A.() -> String): String {
    return arg1.funRef()
}

class A {
    fun foo() = "OK"
}

fun box(): String {
    val x = A::foo
    var r = A().x()
    if (r != "OK") return r

    r = run(A(), A::foo)
    if (r != "OK") return r

    return "OK"
}

