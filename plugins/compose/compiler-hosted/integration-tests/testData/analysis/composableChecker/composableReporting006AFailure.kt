// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

fun foo() {
    val bar = {
        <!COMPOSABLE_INVOCATION!>Leaf<!>()
    }
    bar()
    System.out.println(bar)
}
