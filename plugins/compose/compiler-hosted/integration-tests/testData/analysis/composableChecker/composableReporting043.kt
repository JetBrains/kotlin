// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun FancyButton() {}

fun <!COMPOSABLE_EXPECTED!>Noise<!>() {
    <!COMPOSABLE_INVOCATION!>FancyButton<!>()
}
