// WITH_STDLIB
// WITH_AND_WITHOUT_PLUGIN
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM invokes `println (message: kotlin.Int)` instead of `println (message: kotlin.Any?)`

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun runInlineable(block: @MyInlineable () -> Unit) { block.invoke() }

fun test(): Int {
    var x = 0
    val l: @MyInlineable (() -> Unit) = { x++ }
    val l2 = @MyInlineable { println(x) }

    runInlineable(l)
    runInlineable { x++ }
    runInlineable @MyInlineable { x++ }
    return x
}
fun box(): String {
    val res1 = test()
    if (res1 != 3) return "Fail: $res1"
    return "OK"
}
