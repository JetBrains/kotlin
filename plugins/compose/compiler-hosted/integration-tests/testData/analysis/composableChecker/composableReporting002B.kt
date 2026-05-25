// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

val myLambda1 = @Composable { Leaf() }
val myLambda2: @Composable ()->Unit = { Leaf() }
