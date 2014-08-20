// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun <T> run(arg1: A, arg2: T, funRef:A.(T) -> T): T {
    return arg1.funRef(arg2)
}

class A {
    var xx: Int = 100
}

fun A.bar(x: Int): Int {
    this.xx = this.xx * 2
    return x
}

fun box(): Boolean {
    val funRef = A::bar
    val obj = A()
    var result = obj.(funRef)(25)
    if (result != 25 || obj.xx != 200) return false

    result = run(A(), 25, funRef)
    return result == 25 && obj.xx == 200
}