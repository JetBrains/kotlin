// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun run(arg1: A, funRef:A.() -> Unit): Unit {
    return arg1.funRef()
}

class A {
    var result = "Fail"
    
    fun foo() {
        result = "OK"
    }
}

fun box(): String {
    val a = A()
    val x = A::foo
    a.x()
    var r = a.result
    if (r != "OK") return r

    val a1 = A()
    run(a1, A::foo)
    r = a.result
    if (r != "OK") return r

    return "OK"
}
