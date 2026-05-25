// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

val test1: @Composable () -> Unit = TODO()

@Composable
fun Test2(): Unit = TODO()

@Composable
fun Wrapper(content: @Composable () -> Unit) = content()

@Composable
fun Test3() {
    Wrapper {
        TODO()
    }
}
