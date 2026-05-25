// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
@Composable fun foo() { }
val composable: @Composable ()->Unit = if(true) { { } } else { { foo() } }
