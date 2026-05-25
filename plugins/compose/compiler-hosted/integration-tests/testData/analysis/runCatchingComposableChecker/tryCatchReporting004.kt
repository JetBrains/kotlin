// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
var globalContent = @Composable {}
fun setContent(content: @Composable () -> Unit) {
    globalContent = content
}
@Composable fun A() {}

fun test() {
    runCatching {
        setContent {
            A()
        }
    }
}
