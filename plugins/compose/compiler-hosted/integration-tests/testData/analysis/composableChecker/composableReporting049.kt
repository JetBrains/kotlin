// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
fun foo(<!WRONG_ANNOTATION_TARGET!>@Composable<!> bar: ()->Unit) {
    println(bar)
}
