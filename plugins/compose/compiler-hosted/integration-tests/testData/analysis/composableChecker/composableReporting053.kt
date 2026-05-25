// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun foo(): Int = 123

fun <!COMPOSABLE_EXPECTED!>App<!>() {
    val x = <!COMPOSABLE_INVOCATION!>foo<!>()
    print(x)
}
