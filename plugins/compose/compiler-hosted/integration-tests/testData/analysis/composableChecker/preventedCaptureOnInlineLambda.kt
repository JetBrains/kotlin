// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable inline fun A(
    lambda: @DisallowComposableCalls () -> Unit
) { if (Math.random() > 0.5) lambda() }
@Composable fun B() {}

@Composable fun C() {
    A { <!CAPTURED_COMPOSABLE_INVOCATION!>B<!>() }
}
