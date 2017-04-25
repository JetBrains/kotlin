// EXPECTED_REACHABLE_NODES: 492
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

fun <T> run(arg1: A, arg2: T, funRef:(A, T) -> T): T {
    return funRef(arg1, arg2)
}

class A {
    var xx: Int = 100
}

fun A.bar(x: Int): Int {
    this.xx = this.xx * 2
    return x
}

fun box(): String {
    val funRef = A::bar
    val obj = A()
    var result = funRef(obj, 25)
    if (result != 25 || obj.xx != 200) return "fail1: result = $result, expected 25; obj.xx = $obj.xx, expected 200"

    result = run(A(), 25, funRef)
    if (result != 25 || obj.xx != 200) return "fail2: result = $result, expected 25; obj.xx = $obj.xx, expected 200"

    return "OK"
}
