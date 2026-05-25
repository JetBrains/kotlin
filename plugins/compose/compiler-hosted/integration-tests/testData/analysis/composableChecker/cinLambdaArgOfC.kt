// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun C() { }
@Composable fun C2(lambda: () -> Unit) { lambda() }
@Composable fun C3() {
    C2 {
        <!COMPOSABLE_INVOCATION!>C<!>()
    }
}
