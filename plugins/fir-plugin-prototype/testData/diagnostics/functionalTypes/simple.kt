import org.jetbrains.kotlin.fir.plugin.MyComposable

fun runUsual(block: () -> Unit) {}
fun runComposable(block: @MyComposable () -> Unit) {}

fun test_1() {
    val l0 = {}
    val l1: some.MyComposableFunction0<Unit> = {}
    val l2: @MyComposable (() -> Unit) = {}
    val l3 = @MyComposable {}

    runUsual(l0) // ok
    runUsual(<!ARGUMENT_TYPE_MISMATCH!>l1<!>) // error
    runUsual(<!ARGUMENT_TYPE_MISMATCH!>l2<!>) // error
    runUsual(<!ARGUMENT_TYPE_MISMATCH!>l3<!>) // error
    runUsual {} // ok
    runUsual @MyComposable <!ARGUMENT_TYPE_MISMATCH!>{}<!> // error

    runComposable(l0) // ok
    runComposable(l1) // ok
    runComposable(l2) // ok
    runComposable(l3) // ok
    runComposable {} // ok
    runComposable @MyComposable {} // ok
}

fun runComposable2(block: some.MyComposableFunction1<String, Int>) {}

fun test_2() {
    runComposable2 { it.length }
}
