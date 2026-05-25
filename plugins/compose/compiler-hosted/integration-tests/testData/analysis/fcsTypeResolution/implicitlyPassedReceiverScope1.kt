// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun Int.Foo(content: @Composable Int.() -> Unit) {
    content()
}
