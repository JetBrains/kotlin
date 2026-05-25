// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun C() { }
fun NC(lambda: () -> Unit) { lambda() }
@Composable fun C3() {
    NC {
        <!COMPOSABLE_INVOCATION!>C<!>()
    }
}
