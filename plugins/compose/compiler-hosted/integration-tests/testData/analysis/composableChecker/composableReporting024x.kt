// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

var x: (@Composable () -> Unit)? = null

fun <!COMPOSABLE_EXPECTED!>Example<!>(content: @Composable () -> Unit) {
    x = content
    <!COMPOSABLE_INVOCATION!>content<!>()
}
