// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
val composable: @Composable ()->Unit = if(true) { { } } else { { } }
