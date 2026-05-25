// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun Foo() {}

val <!COMPOSABLE_EXPECTED!>bam<!>: Int get() {
    <!COMPOSABLE_INVOCATION!>Foo<!>()
    return 123
}
