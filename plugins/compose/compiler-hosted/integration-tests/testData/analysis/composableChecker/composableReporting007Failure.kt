// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

fun <!COMPOSABLE_EXPECTED!>foo<!>(content: @Composable ()->Unit) {
    <!COMPOSABLE_INVOCATION!>content<!>()
}
