// DUMP_IR

import org.jetbrains.kotlin.fir.plugin.MyComposable

fun runUsual(block: () -> Unit) { block.invoke() }
fun runComposable(block: @MyComposable () -> Unit) { block.invoke() }

fun test_1(): Int {
    var x = 0
    val l0 = { x++; Unit }
    val l1: some.MyComposableFunction0<Unit> = { x++; Unit }
    val l2: @MyComposable (() -> Unit) = { x++ }
    val l3 = @MyComposable { x++; Unit }

    runUsual(l0)
    runUsual { x++ }

    runComposable(l0)
    runComposable(l1)
    runComposable(l2)
    runComposable(l3)
    runComposable { x++ }
    runComposable @MyComposable { x++ }
    return x
}

fun runComposable2(block: some.MyComposableFunction1<String, String>): String {
    return block.invoke("O")
}

fun test_2(): String {
    return runComposable2 { it + "K" }
}

fun box(): String {
    val res1 = test_1()
    if (res1 != 8) return "Fail 1: $res1"
    val res2 = test_2()
    if (res2 != "OK") return "Fail 2: $res2"
    return "OK"
}
