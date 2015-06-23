// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

class A {
    fun result() = (::bar)(this, "OK")
}

fun A.bar(x: String) = x

fun box() = A().result()
