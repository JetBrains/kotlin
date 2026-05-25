// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

val foo: Int @Composable  get() = 123

@Composable
fun App() {
    println(foo)
}
