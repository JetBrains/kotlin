// INFERENCE_HELPERS
import org.jetbrains.kotlin.fir.plugin.MyComposable

fun <T : suspend () -> Unit> suspendId(f: T): T = f
fun <T : @MyComposable () -> Unit> composableId(f: T): T = f

fun test_1() {
    <!DEBUG_INFO_EXPRESSION_TYPE("some.MyComposableFunction0<kotlin.Unit>")!>id(@MyComposable {})<!>
    val f: () -> Unit = id(@MyComposable <!ARGUMENT_TYPE_MISMATCH!>{}<!>) // should be an error
    <!DEBUG_INFO_EXPRESSION_TYPE("some.MyComposableFunction0<kotlin.Unit>")!>composableId({})<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("some.MyComposableFunction0<kotlin.Unit>")!>composableId(@MyComposable {})<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction0<kotlin.Unit>")!>suspendId(@MyComposable <!ARGUMENT_TYPE_MISMATCH!>{}<!>)<!>
}

fun test_2() {
    <!DEBUG_INFO_EXPRESSION_TYPE("some.MyComposableFunction0<kotlin.String>")!>select(@MyComposable { "a" }, { "b" })<!>
}
