// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

val lambda: @Composable (() -> Unit)? = null

@Composable
fun Foo() {
    Bar(lambda)
    Bar(null)
    Bar {}
}

@Composable
fun Bar(child: @Composable (() -> Unit)?) {
    child?.invoke()
}
