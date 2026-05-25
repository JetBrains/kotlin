// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

val foo: Int @Composable get() = 123

fun <!COMPOSABLE_EXPECTED!>App<!>() {
    <!COMPOSABLE_INVOCATION!>foo<!>
}
