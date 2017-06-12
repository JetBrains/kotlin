// EXPECTED_REACHABLE_NODES: 492
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A {
    var result = "Fail"
    
    fun foo(newResult: String) {
        result = newResult
    }
}

fun box(): String {
    val a = A()
    val x = A::foo
    x(a, "OK")
    return a.result
}
