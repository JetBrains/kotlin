// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun C() { }
inline fun CrossinlineNC(crossinline lambda: () -> Unit) { lambda() }
@Composable fun C3() {
    CrossinlineNC {
        <!COMPOSABLE_INVOCATION!>C<!>()
    }
}
