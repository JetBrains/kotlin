// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun Leaf() {}

@Composable
fun Foo(content: ()->Unit) {
    content()
}

@Composable
fun test() {
    Foo { <!COMPOSABLE_INVOCATION!>Leaf<!>() }
}
