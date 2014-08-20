// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun run(arg: Int, funRef:(Int) -> Int): Int {
    return funRef(arg)
}

fun inc(x: Int) = x + 1

fun tmp():Function1<Int, Int> {
    return ::inc
}

fun box(): Boolean {
    if (tmp()(5) != 6) return false

    if (run(5, tmp()) != 6) return false

    return true
}