// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

class A {
    var result = "OK"
}

fun box() = (::A)().result
