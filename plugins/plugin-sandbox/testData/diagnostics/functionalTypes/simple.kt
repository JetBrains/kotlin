import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun runUsual(block: () -> Unit) {}
fun runInlineable(block: @MyInlineable () -> Unit) {}

fun test_1() {
    val l0 = {}
    val l1: some.MyInlineableFunction0<Unit> = {}
    val l2: @MyInlineable (() -> Unit) = {}
    val l3 = @MyInlineable {}

    runUsual(l0) // ok
    runUsual(<!ARGUMENT_TYPE_MISMATCH!>l1<!>) // error
    runUsual(<!ARGUMENT_TYPE_MISMATCH!>l2<!>) // error
    runUsual(<!ARGUMENT_TYPE_MISMATCH!>l3<!>) // error
    runUsual {} // ok
    runUsual @MyInlineable <!ARGUMENT_TYPE_MISMATCH!>{}<!> // error

    runInlineable(l0) // ok
    runInlineable(l1) // ok
    runInlineable(l2) // ok
    runInlineable(l3) // ok
    runInlineable {} // ok
    runInlineable @MyInlineable {} // ok
}

fun runInlineable2(block: some.MyInlineableFunction1<String, Int>) {}

fun test_2() {
    runInlineable2 { it.length }
}
