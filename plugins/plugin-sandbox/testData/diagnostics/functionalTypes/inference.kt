// INFERENCE_HELPERS
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun <T : suspend () -> Unit> suspendId(f: T): T = f
fun <T : @MyInlineable () -> Unit> InlineableId(f: T): T = f

fun test_1() {
    <!DEBUG_INFO_EXPRESSION_TYPE("some.MyInlineableFunction0<kotlin.Unit>")!>id(@MyInlineable {})<!>
    val f: () -> Unit = id(@MyInlineable <!ARGUMENT_TYPE_MISMATCH!>{}<!>) // should be an error
    <!DEBUG_INFO_EXPRESSION_TYPE("some.MyInlineableFunction0<kotlin.Unit>")!>InlineableId({})<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("some.MyInlineableFunction0<kotlin.Unit>")!>InlineableId(@MyInlineable {})<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction0<kotlin.Unit>")!>suspendId(@MyInlineable <!ARGUMENT_TYPE_MISMATCH!>{}<!>)<!>
}

fun test_2() {
    <!DEBUG_INFO_EXPRESSION_TYPE("some.MyInlineableFunction0<kotlin.String>")!>select(@MyInlineable { "a" }, { "b" })<!>
}
