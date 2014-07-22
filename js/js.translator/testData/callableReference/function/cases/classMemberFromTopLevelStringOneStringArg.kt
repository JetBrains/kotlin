// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun run(arg1: A, arg2: String, funRef:A.(String) -> String): String {
    return arg1.funRef(arg2)
}

class A {
    fun foo(result: String):String = result
}

fun box(): String {
    val x = A::foo
    var r = A().x("OK")

    if (r != "OK") return r

    r = run(A(), "OK", A::foo)
    if (r != "OK") return r
    return "OK"
}
