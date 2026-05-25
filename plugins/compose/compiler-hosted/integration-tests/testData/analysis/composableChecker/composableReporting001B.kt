// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

@Composable
fun myStatelessFunctionalComponent() {
    Leaf()
}

@Composable
fun foo() {
    myStatelessFunctionalComponent()
}
