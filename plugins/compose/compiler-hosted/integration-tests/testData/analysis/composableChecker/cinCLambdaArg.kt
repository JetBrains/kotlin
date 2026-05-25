// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun C() { }
@Composable fun C2(lambda: @Composable () -> Unit) { lambda() }
@Composable fun C3() {
    C2 {
        C()
    }
}
