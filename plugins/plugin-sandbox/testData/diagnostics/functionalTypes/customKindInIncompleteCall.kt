// ISSUE: KT-72664

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun foo(
    f: @MyInlineable () -> Unit,
    g: @MyInlineable () -> Unit,
    h: @MyInlineable () -> Unit,
) {}


fun test_good() {
    foo(
        f = <!DEBUG_INFO_EXPRESSION_TYPE("some.MyInlineableFunction0<kotlin.Unit>")!>{}<!>,
        g = <!DEBUG_INFO_EXPRESSION_TYPE("some.MyInlineableFunction0<kotlin.Unit>")!>{}<!>,
        h = <!DEBUG_INFO_EXPRESSION_TYPE("some.MyInlineableFunction0<kotlin.Unit>")!>{}<!>,
    )
}

fun test_bad() {
    foo(
        f = <!DEBUG_INFO_EXPRESSION_TYPE("some.MyInlineableFunction0<kotlin.Unit>")!>{}<!>,
        <!NO_VALUE_FOR_PARAMETER!>g = <!DEBUG_INFO_EXPRESSION_TYPE("some.MyInlineableFunction0<kotlin.Unit>")!>{}<!>,
    )<!>
}
