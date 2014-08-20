// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun run(arg: Int, funRef:(Int) -> Int): Int {
    return funRef(arg)
}
fun inc(x: Int) = x + 1

fun box(): Boolean {
    val funRef = ::inc
    if (funRef(5) != 6) return false

    if (run(5, funRef) != 6) return false

    if (run(5) {x -> x + 1} != 6) return false

    return true
}