// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

fun foo() {
    val lambda = @Composable { }
    println(lambda)  // println accepts Any, verify no type mismatach.
}
