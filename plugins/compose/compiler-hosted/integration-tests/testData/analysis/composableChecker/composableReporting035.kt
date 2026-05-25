// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun Foo(x: String) {
    @Composable operator fun String.invoke() {}
    x()
}
