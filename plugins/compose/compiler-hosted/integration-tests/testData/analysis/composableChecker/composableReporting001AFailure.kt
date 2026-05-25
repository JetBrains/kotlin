// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

fun <!COMPOSABLE_EXPECTED!>myStatelessFunctionalComponent<!>() {
    <!COMPOSABLE_INVOCATION!>Leaf<!>()
}
