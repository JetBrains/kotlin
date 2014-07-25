// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun run(arg1: A, arg2: String, funRef:A.(String) -> Unit): Unit {
    return arg1.funRef(arg2)
}

class A {
    var result = "Fail"
    
    fun foo(newResult: String) {
        result = newResult
    }
}

fun box(): String {
    val a = A()
    val x = A::foo
    a.x("OK")
    var r = a.result
    if (r != "OK") return r

    val a1 = A()
    run(a1, "OK", A::foo)
    r = a1.result
    if (r != "OK") return r

    return "OK"
}
