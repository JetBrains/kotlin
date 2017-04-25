// EXPECTED_REACHABLE_NODES: 491
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

fun run(arg: Int, funRef:(Int) -> Int): Int {
    return funRef(arg)
}
fun inc(x: Int) = x + 1

fun box(): String {
    val funRef = ::inc
    if (funRef(5) != 6) return "fail1"

    if (run(5, funRef) != 6) return "fail2"

    if (run(5) {x -> x + 1} != 6) return "fail3"

    return "OK"
}
