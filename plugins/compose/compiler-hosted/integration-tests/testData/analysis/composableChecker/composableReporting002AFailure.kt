// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

val myLambda1 = { <!COMPOSABLE_INVOCATION!>Leaf<!>() }
val myLambda2: () -> Unit = { <!COMPOSABLE_INVOCATION!>Leaf<!>() }
