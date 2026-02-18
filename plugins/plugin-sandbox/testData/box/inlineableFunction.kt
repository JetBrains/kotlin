// DUMP_IR

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun runUsual(block: () -> Unit) { block.invoke() }
fun runInlineable(block: @MyInlineable () -> Unit) { block.invoke() }

fun test_1(): Int {
    var x = 0
    val l0 = { x++; Unit }
    val l1: some.MyInlineableFunction0<Unit> = { x++; Unit }
    val l2: @MyInlineable (() -> Unit) = { x++ }
    val l3 = @MyInlineable { x++; Unit }

    runUsual(l0)
    runUsual { x++ }

    runInlineable(l0)
    runInlineable(l1)
    runInlineable(l2)
    runInlineable(l3)
    runInlineable { x++ }
    runInlineable @MyInlineable { x++ }
    return x
}

fun runInlineable2(block: some.MyInlineableFunction1<String, String>): String {
    return block.invoke("O")
}

fun test_2(): String {
    return runInlineable2 { it + "K" }
}

fun box(): String {
    val res1 = test_1()
    if (res1 != 8) return "Fail 1: $res1"
    val res2 = test_2()
    if (res2 != "OK") return "Fail 2: $res2"
    return "OK"
}
