// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun Leaf() {}

@Composable
fun myStatelessFunctionalComponent() {
    Leaf()
}

fun <!COMPOSABLE_EXPECTED!>noise<!>() {
    <!COMPOSABLE_INVOCATION!>myStatelessFunctionalComponent<!>()
}
