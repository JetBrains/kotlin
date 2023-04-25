import org.jetbrains.kotlin.fir.plugin.*

fun consumeRegularFunction(block: () -> Unit) {}
fun consumeSuspendFunction(block: suspend () -> Unit) {}
fun consumeOurComposableFunction(block: @MyComposable () -> Unit) {}

fun test_1(
    block: () -> Unit,
    composableBlock: @MyComposable () -> Unit,
    suspendBlock: suspend () -> Unit,
) {
    consumeComposableFunction(block)
    consumeComposableFunction(composableBlock)
    consumeComposableFunction(<!ARGUMENT_TYPE_MISMATCH!>suspendBlock<!>) // should be error
}

fun test_2() {
    val block = produceComposableFunction()
    consumeRegularFunction(<!ARGUMENT_TYPE_MISMATCH!>block<!>) // should be error
    consumeSuspendFunction(<!ARGUMENT_TYPE_MISMATCH!>block<!>) // should be error
    consumeOurComposableFunction(block)
    consumeComposableFunction(block)
}

fun test_3() {
    val block = produceBoxedComposableFunction().value
    consumeRegularFunction(<!ARGUMENT_TYPE_MISMATCH!>block<!>) // should be error
    consumeSuspendFunction(<!ARGUMENT_TYPE_MISMATCH!>block<!>) // should be error
    consumeOurComposableFunction(block)
    consumeComposableFunction(block)
}
