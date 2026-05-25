// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun Foo(bar: String) { print(bar) }

@Composable
fun test(bar: String?) {
    Foo(<!ARGUMENT_TYPE_MISMATCH!>bar<!>)
    if (bar != null) {
        Foo(bar)
        Foo(bar=bar)
    }
}
