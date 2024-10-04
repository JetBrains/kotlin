import org.jetbrains.kotlin.plugin.sandbox.*

fun consumeRegularFunction(block: () -> Unit) {}
fun consumeSuspendFunction(block: suspend () -> Unit) {}
fun consumeOurInlineableFunction(block: @MyInlineable () -> Unit) {}

fun test_1(
    block: () -> Unit,
    InlineableBlock: @MyInlineable () -> Unit,
    suspendBlock: suspend () -> Unit,
) {
    consumeInlineableFunction(block)
    consumeInlineableFunction(InlineableBlock)
    consumeInlineableFunction(<!ARGUMENT_TYPE_MISMATCH!>suspendBlock<!>) // should be error
}

fun test_2() {
    val block = produceInlineableFunction()
    consumeRegularFunction(<!ARGUMENT_TYPE_MISMATCH!>block<!>) // should be error
    consumeSuspendFunction(<!ARGUMENT_TYPE_MISMATCH!>block<!>) // should be error
    consumeOurInlineableFunction(block)
    consumeInlineableFunction(block)
}

fun test_3() {
    val block = produceBoxedInlineableFunction().value
    consumeRegularFunction(<!ARGUMENT_TYPE_MISMATCH!>block<!>) // should be error
    consumeSuspendFunction(<!ARGUMENT_TYPE_MISMATCH!>block<!>) // should be error
    consumeOurInlineableFunction(block)
    consumeInlineableFunction(block)
}
