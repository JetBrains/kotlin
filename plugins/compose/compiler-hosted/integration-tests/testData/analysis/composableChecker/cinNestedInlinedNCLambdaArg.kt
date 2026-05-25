// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun C() { }
inline fun InlineNC(lambda: () -> Unit) { lambda() }
@Composable fun C3() {
    InlineNC {
        InlineNC {
            C()
        }
    }
}
